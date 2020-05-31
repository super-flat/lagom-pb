package lagompb

import akka.actor.ActorSystem
import lagompb.protobuf.core.{CommandHandlerResponse, MetaData}

import scala.util.Try

/**
 * LagompbCommandHandler
 *
 * @param actorSystem the actor system
 * @tparam TState the aggregate state type
 */
abstract class LagompbCommandHandler[TState <: scalapb.GeneratedMessage](actorSystem: ActorSystem) {

  /**
   * Handles a given command send to the entity
   *
   * @param command   the actual to handle
   * @param currentState     the current aggregate state before the command was triggered
   * @param currentMetaData the current event meta before the command was triggered
   * @return CommandHandlerResponse
   */
  def handle(command: LagompbCommand, currentState: TState, currentMetaData: MetaData): Try[CommandHandlerResponse]
}

/**
 * LagomPbEventHandler
 *
 * @param actorSystem the actor system
 * @tparam TState the aggregate state type
 */
abstract class LagompbEventHandler[TState <: scalapb.GeneratedMessage](actorSystem: ActorSystem) {

  /**
   * Handles a given event ad return the resulting state
   *
   * @param event the event to handle
   * @param currentState the current state
   * @param metaData the event meta characterising the actual event.
   * @return the resulting state
   */
  def handle(event: scalapb.GeneratedMessage, currentState: TState, metaData: MetaData): TState
}
