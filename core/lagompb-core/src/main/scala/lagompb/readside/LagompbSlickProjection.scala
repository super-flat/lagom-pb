package lagompb.readside

import akka.projection.eventsourced.EventEnvelope
import akka.Done
import akka.actor.{ActorSystem => ActorSystemClassic}
import com.google.protobuf.any
import com.typesafe.config.Config
import lagompb.{LagompbEvent, LagompbException}
import lagompb.protobuf.core.{EventWrapper, MetaData}
import lagompb.util.LagompbProtosCompanions
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}
import slick.dbio.{DBIO, DBIOAction}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/**
 * LagompbSlickProjection helps implement multiple readSide processors where the offsets are
 * persisted in a RDBMS of choice. One of the greatest advantage is one can process events emitted differently by
 * spawning different type of [[LagompbSlickProjection]] to handle them.
 * Each instance must be registered in the [[lagompb.LagompbApplication]] via
 * the dependency injection and the init method called
 *
 * Please bear in mind that the akka.projection.slick is required to be set in the configuration file.
 *
 * @see https://doc.akka.io/docs/akka-projection/current/slick.html#configuration
 * @param config      the configuration instance
 * @param actorSystem the actor system
 * @param projectionName the projectionName must be unique per projection instance
 * @param ec          the execution context
 * @tparam TState the aggregate state type
 */
abstract class LagompbSlickProjection[TState <: scalapb.GeneratedMessage](
    config: Config,
    actorSystem: ActorSystemClassic,
    projectionName: String
)(implicit ec: ExecutionContext)
    extends LagompbProjection[TState](config, actorSystem, projectionName) {

  override def process(envelope: EventEnvelope[LagompbEvent]): DBIO[Done] = {
    envelope.event match {
      case EventWrapper(Some(event: any.Any), Some(resultingState), Some(meta)) =>
        LagompbProtosCompanions
          .getCompanion(event)
          .fold[DBIO[Done]](
            DBIOAction.failed(new LagompbException(s"[Lagompb] unable to parse event ${event.typeUrl}"))
          )((comp: GeneratedMessageCompanion[_ <: GeneratedMessage]) => {
            Try {
              handle(
                event.unpack(comp),
                resultingState
                  .unpack[TState](aggregateStateCompanion),
                meta
              )
            } match {
              case Failure(exception) =>
                DBIOAction.failed(exception)
              case Success(result: Any) =>
                result
            }
          })
      case _ =>
        DBIO.failed(new LagompbException(s"[Lagompb] unknown event received ${envelope.event.getClass.getName}"))
    }
  }

  /**
   * Handles aggregate event persisted and made available for read model
   *
   * @param event the aggregate event
   * @param state the Lagompb state that wraps the actual state and some meta data
   */
  def handle(event: scalapb.GeneratedMessage, state: TState, metaData: MetaData): DBIO[Done]
}
