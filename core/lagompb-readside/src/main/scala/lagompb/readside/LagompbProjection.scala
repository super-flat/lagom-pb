package lagompb.readside

import akka.actor.{ActorSystem => ActorSystemClassic}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.Offset
import akka.projection.{ProjectionBehavior, ProjectionId}
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.SourceProvider
import akka.projection.slick.{SlickHandler, SlickProjection}
import akka.Done
import com.github.ghik.silencer.silent
import com.google.protobuf.any
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag
import com.typesafe.config.Config
import lagompb.{LagompbEvent, LagompbException, LagompbProtosRegistry}
import lagompb.core.{EventWrapper, MetaData}
import org.slf4j.{Logger, LoggerFactory}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}
import slick.basic.DatabaseConfig
import slick.dbio.{DBIO, DBIOAction}
import slick.jdbc.PostgresProfile

import scala.concurrent.ExecutionContext

@silent abstract class LagompbProjection[TState <: scalapb.GeneratedMessage](
    config: Config,
    actorSystem: ActorSystemClassic
)(implicit ec: ExecutionContext)
    extends SlickHandler[EventEnvelope[LagompbEvent]] {

  final val log: Logger = LoggerFactory.getLogger(getClass)

  protected val actorSystemTyped: ActorSystem[_] = {
    actorSystem.toTyped
  }

  // The implementation class needs to set the akka.projection.slick config for the offset database
  protected val dbConfig: DatabaseConfig[PostgresProfile] =
    DatabaseConfig.forConfig("akka.projection.slick", actorSystem.settings.config)
  protected val baseTag: String = config.getString("lagompb.events.tagname")

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
    ShardedDaemonProcess(actorSystemTyped).init[ProjectionBehavior.Command](
      name = projectionName,
      numberOfInstances = LagompbEvent.Tag.allTags.size,
      behaviorFactory = n => ProjectionBehavior(setExactlyOnceProjection(AggregateEventTag.shardTag(baseTag, n))),
      settings = ShardedDaemonProcessSettings(actorSystemTyped),
      stopMessage = Some(ProjectionBehavior.Stop)
    )
  }

  /**
   * Build the projection instance based upon the event tag
   *
   * @param tag the event tag
   * @return the projection instance
   */
  protected def setExactlyOnceProjection(tag: String): SlickProjection[EventEnvelope[LagompbEvent]] =
    SlickProjection
      .exactlyOnce(projectionId = ProjectionId(projectionName, tag), setSourceProvider(tag), dbConfig, handler = this)

  /**
   * Set the Event Sourced Provider per tag
   *
   * @param tag the event tag
   * @return the event sourced provider
   */
  protected def setSourceProvider(tag: String): SourceProvider[Offset, EventEnvelope[LagompbEvent]] =
    EventSourcedProvider
      .eventsByTag[LagompbEvent](actorSystemTyped, readJournalPluginId = JdbcReadJournal.Identifier, tag)

  /**
   * handles the actual event unmarshalled from the event wrapper
   *
   * @param comp the companion object of the event unmarshalled
   * @param event the actual event
   * @param resultingState the resulting state
   * @param meta the meta data
   * @return
   */
  def handleEvent(
      comp: GeneratedMessageCompanion[_ <: GeneratedMessage],
      event: any.Any,
      resultingState: any.Any,
      meta: MetaData
  ): DBIO[Done]

  final override def process(envelope: EventEnvelope[LagompbEvent]): DBIO[Done] = {
    envelope.event match {
      case EventWrapper(Some(event: any.Any), Some(resultingState), Some(meta)) =>
        LagompbProtosRegistry
          .getCompanion(event) match {
          case Some(comp) =>
            handleEvent(comp, event, resultingState, meta)
          case None => DBIOAction.failed(new LagompbException(s"companion not found for ${event.typeUrl}"))
        }
      case _ =>
        DBIO.failed(new LagompbException(s"[Lagompb] unknown event received ${envelope.event.getClass.getName}"))
    }
  }
}
