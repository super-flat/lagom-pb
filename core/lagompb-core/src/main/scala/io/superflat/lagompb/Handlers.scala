package io.superflat.lagompb

import akka.actor.ActorSystem
import io.superflat.lagompb.v1.protobuf.core.{CommandHandlerResponse, MetaData}

import scala.util.Try

/**
 * LagompbCommandHandler
 *
 * @param actorSystem the actor system
 * @tparam S the aggregate state type
 */
abstract class CommandHandler[S <: scalapb.GeneratedMessage](actorSystem: ActorSystem) {

  /**
   * Handles a given command send to the entity
   *
   * @param command   the actual to handle
   * @param currentState     the current aggregate state before the command was triggered
   * @param currentMetaData the current event meta before the command was triggered
   * @return CommandHandlerResponse
   */
  def handle(command: Command, currentState: S, currentMetaData: MetaData): Try[CommandHandlerResponse]
}

/**
 * LagomPbEventHandler
 *
 * @param actorSystem the actor system
 * @tparam S the aggregate state type
 */
abstract class EventHandler[S <: scalapb.GeneratedMessage](actorSystem: ActorSystem) {

  /**
   * Handles a given event ad return the resulting state
   *
   * @param event the event to handle
   * @param currentState the current state
   * @param metaData the event meta characterising the actual event.
   * @return the resulting state
   */
  def handle(event: scalapb.GeneratedMessage, currentState: S, metaData: MetaData): S
}
