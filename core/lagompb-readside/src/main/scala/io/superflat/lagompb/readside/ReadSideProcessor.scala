/*
 * Copyright (C) 2020 Superflat. <https://github.com/super-flat>
 */

package io.superflat.lagompb.readside

import akka.Done
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.Offset
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.{ExactlyOnceProjection, SourceProvider}
import akka.projection.slick.SlickProjection
import akka.projection.{ProjectionBehavior, ProjectionId}
import com.github.ghik.silencer.silent
import com.google.protobuf.any.Any
import io.superflat.lagompb.ConfigReader
import io.superflat.lagompb.encryption.EncryptionAdapter
import io.superflat.lagompb.protobuf.v1.core.{EventWrapper, MetaData}
import org.slf4j.{Logger, LoggerFactory}
import slick.basic.DatabaseConfig
import slick.dbio.{DBIO, DBIOAction}
import slick.jdbc.PostgresProfile

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/**
 * ReadSideProcessor helps implement multiple readSide processors where the offsets are
 * persisted postgres. One of the greatest advantage is one can process events emitted differently by
 * spawning different type of [[ReadSideProcessor]] to handle them.
 * Each instance must be registered in the application via
 * the dependency injection and the init method called
 *
 * Please bear in mind that the akka.projection.slick is required to be set in the configuration file.
 *
 * @see https://doc.akka.io/docs/akka-projection/current/slick.html#configuration
 * @param encryptionAdapter EncryptionAdapter instance to use
 * @param actorSystem the actor system
 * @param ec          the execution context
 */
@silent abstract class ReadSideProcessor(encryptionAdapter: EncryptionAdapter)(implicit
    ec: ExecutionContext,
    actorSystem: ActorSystem[_]
) extends EventProcessor {

  final val log: Logger = LoggerFactory.getLogger(getClass)

  // The implementation class needs to set the akka.projection.slick config for the offset database
  protected val offsetStoreDatabaseConfig: DatabaseConfig[PostgresProfile] =
    DatabaseConfig.forConfig("akka.projection.slick", actorSystem.settings.config)

  protected val baseTag: String = ConfigReader.eventsConfig.tagName

  final override def process(
      event: Any,
      eventTag: String,
      resultingState: Any,
      meta: MetaData
  ): DBIO[Done] =
    Try {
      handle(
        ReadSideEvent(
          event,
          eventTag,
          resultingState,
          meta
        )
      )
    } match {
      case Failure(exception) =>
        DBIOAction.failed(exception)
      case Success(result) =>
        result
    }

  /**
   * Initialize the projection to start fetching the events that are emitted
   */
  def init(): Unit = {
    // Let us attempt to create the projection store
    if (ConfigReader.createOffsetStore) SlickProjection.createOffsetTableIfNotExists(offsetStoreDatabaseConfig)

    ShardedDaemonProcess(actorSystem).init[ProjectionBehavior.Command](
      name = projectionName,
      numberOfInstances = ConfigReader.allEventTags.size,
      behaviorFactory = n => ProjectionBehavior(exactlyOnceProjection(s"$baseTag$n")),
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
  protected def exactlyOnceProjection(tagName: String): ExactlyOnceProjection[Offset, EventEnvelope[EventWrapper]] = {
    SlickProjection
      .exactlyOnce(
        projectionId = ProjectionId(projectionName, tagName),
        sourceProvider(tagName),
        offsetStoreDatabaseConfig,
        handler = () => new EventsReader(tagName, this, encryptionAdapter)
      )
  }

  /**
   * Set the Event Sourced Provider per tag
   *
   * @param tag the event tag
   * @return the event sourced provider
   */
  protected def sourceProvider(tag: String): SourceProvider[Offset, EventEnvelope[EventWrapper]] = {
    EventSourcedProvider
      .eventsByTag[EventWrapper](actorSystem, readJournalPluginId = JdbcReadJournal.Identifier, tag)
  }

  /**
   * The projection Name must be unique
   *
   * @return
   */
  def projectionName: String

  /**
   * Handles aggregate event persisted and made available for read model
   *
   * @param event the aggregate event
   */
  def handle(event: ReadSideEvent): DBIO[Done]
}
