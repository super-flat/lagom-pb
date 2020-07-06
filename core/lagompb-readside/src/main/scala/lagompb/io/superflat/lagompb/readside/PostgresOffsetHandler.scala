package io.superflat.lagompb.readside

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.Offset
import akka.projection.{ProjectionBehavior, ProjectionId}
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.{ExactlyOnceProjection, SourceProvider}
import akka.projection.slick.{SlickHandler, SlickProjection}
import akka.Done
import com.github.ghik.silencer.silent
import com.google.protobuf.any
import io.superflat.lagompb.{ConfigReader, GlobalException, ProtosRegistry}
import io.superflat.lagompb.encryption.{DecryptPermanentFailure, ProtoEncryption}
import io.superflat.lagompb.protobuf.core.{EventWrapper, MetaData}
import io.superflat.lagompb.protobuf.encryption.EncryptedProto
import org.slf4j.{Logger, LoggerFactory}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}
import slick.basic.DatabaseConfig
import slick.dbio.{DBIO, DBIOAction}
import slick.jdbc.PostgresProfile

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/**
 * PostgresOffsetHandler helps manage akka projections offsets in postgres whether events are persisted
 * in an RDBMS or in Kafka.
 *
 * Please bear in mind that the akka.projection.slick is required to be set in the configuration file.
 *
 * @param encryption  the encryption handler
 * @param ec          the execution context
 * @param actorSystem the typed actor system
 * @tparam TState the aggregate state type param
 */
@silent abstract class PostgresOffsetHandler[TState <: scalapb.GeneratedMessage](encryption: ProtoEncryption)(implicit
    ec: ExecutionContext,
    actorSystem: ActorSystem[_]
) extends SlickHandler[EventEnvelope[EncryptedProto]] {

  final val log: Logger = LoggerFactory.getLogger(getClass)

  // The implementation class needs to set the akka.projection.slick config for the offset database
  protected val dbConfig: DatabaseConfig[PostgresProfile] =
    DatabaseConfig.forConfig("akka.projection.slick", actorSystem.settings.config)
  protected val baseTag: String = ConfigReader.eventsConfig.tagName

  /**
   * aggregate state. it is a generated scalapb message extending the LagompbState trait
   *
   * @return aggregate state
   */
  def aggregateStateCompanion: scalapb.GeneratedMessageCompanion[TState]

  /**
   * The projection Name must be unique
   *
   * @return
   */
  def projectionName: String

  /**
   * Initialize the projection to start fetching the events that are emitted
   */
  def init(): Unit = {
    ShardedDaemonProcess(actorSystem).init[ProjectionBehavior.Command](
      name = projectionName,
      numberOfInstances = ConfigReader.allEventTags.size,
      behaviorFactory = n => ProjectionBehavior(setExactlyOnceProjection(s"$baseTag$n")),
      settings = ShardedDaemonProcessSettings(actorSystem),
      stopMessage = Some(ProjectionBehavior.Stop)
    )
  }

  /**
   * Build the projection instance based upon the event tag
   *
   * @param tagName the event tag
   * @return the projection instance
   */

  protected def setExactlyOnceProjection(
      tagName: String
  ): ExactlyOnceProjection[Offset, EventEnvelope[EncryptedProto]] =
    SlickProjection
      .exactlyOnce(
        projectionId = ProjectionId(projectionName, tagName),
        setSourceProvider(tagName),
        dbConfig,
        handler = () => this
      )

  /**
   * Set the Event Sourced Provider per tag
   *
   * @param tag the event tag
   * @return the event sourced provider
   */
  protected def setSourceProvider(tag: String): SourceProvider[Offset, EventEnvelope[EncryptedProto]] =
    EventSourcedProvider
      .eventsByTag[EncryptedProto](actorSystem, readJournalPluginId = JdbcReadJournal.Identifier, tag)

  /**
   * handles the actual event unmarshalled from the event wrapper
   *
   * @param comp           the companion object of the event unmarshalled
   * @param event          the actual event
   * @param resultingState the resulting state
   * @param meta           the meta data
   * @return
   */
  def handleEvent(
      comp: GeneratedMessageCompanion[_ <: GeneratedMessage],
      event: any.Any,
      resultingState: any.Any,
      meta: MetaData
  ): DBIO[Done]

  final override def process(envelope: EventEnvelope[EncryptedProto]): DBIO[Done] = {

    encryption
    // decrypt the message into an Any
      .decrypt(envelope.event)
      // unpack into the EventWrapper
      .map(_.unpack(EventWrapper))
      // handle happy path decryption
      .map({
        case EventWrapper(Some(event: any.Any), Some(resultingState), Some(meta)) =>
          ProtosRegistry.getCompanion(event) match {
            case Some(comp) =>
              handleEvent(comp, event, resultingState, meta)

            case None =>
              DBIOAction.failed(new GlobalException(s"companion not found for ${event.typeUrl}"))
          }

        case _ =>
          DBIO.failed(new GlobalException(s"[Lagompb] unknown event received ${envelope.event.getClass.getName}"))
      })
      .recoverWith({
        case DecryptPermanentFailure(reason) =>
          log.debug(s"skipping offset with reason, $reason")
          Try(DBIOAction.successful(Done))

        case throwable: Throwable =>
          log.error("failed to handle event", throwable)
          Try(DBIO.failed(throwable))
      }) match {
      case Success(value) => value
      case Failure(exception) => throw exception
    }
  }
}
