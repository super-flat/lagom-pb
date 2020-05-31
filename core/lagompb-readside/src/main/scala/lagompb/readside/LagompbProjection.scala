package lagompb.readside

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ActorSystem => ActorSystemClassic}
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.Offset
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.SourceProvider
import akka.projection.slick.{SlickHandler, SlickProjection}
import akka.projection.{ProjectionBehavior, ProjectionId}
import com.github.ghik.silencer.silent
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag
import com.typesafe.config.Config
import lagompb.LagompbEvent
import slick.basic.DatabaseConfig
import slick.jdbc.PostgresProfile

import scala.concurrent.ExecutionContext

@silent abstract class LagompbProjection[TState <: scalapb.GeneratedMessage](
    config: Config,
    actorSystem: ActorSystemClassic
)(implicit ec: ExecutionContext)
    extends SlickHandler[EventEnvelope[LagompbEvent]] {

  /**
   * aggregate state. it is a generated scalapb message extending the LagompbState trait
   *
   * @return aggregate state
   */
  def aggregateStateCompanion: scalapb.GeneratedMessageCompanion[TState]

  /**
   * The projection Name must be unique
   * @return
   */
  def projectionName: String

  protected val actorSystemTyped: ActorSystem[_] = {
    actorSystem.toTyped
  }

  // The implementation class needs to set the akka.projection.slick config for the offset database
  protected val dbConfig: DatabaseConfig[PostgresProfile] =
    DatabaseConfig.forConfig("akka.projection.slick", actorSystem.settings.config)

  protected val baseTag: String = config.getString("lagompb.events.tagname")

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
   * Build the projection instance based upon the event tag
   *
   * @param tag the event tag
   * @return the projection instance
   */
  protected def setExactlyOnceProjection(tag: String): SlickProjection[EventEnvelope[LagompbEvent]] =
    SlickProjection
      .exactlyOnce(projectionId = ProjectionId(projectionName, tag), setSourceProvider(tag), dbConfig, handler = this)

  /**
   * Initialize the projection
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

}
