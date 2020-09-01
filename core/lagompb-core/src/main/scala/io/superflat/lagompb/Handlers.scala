package io.superflat.lagompb

import akka.actor.ActorSystem
import com.google.protobuf.any.Any
import io.superflat.lagompb.protobuf.v1.core.{CommandHandlerResponse, MetaData}

import scala.util.{Failure, Success, Try}

trait CommandHandler {
  def handle(command: Any, currentState: Any, currentMetaData: MetaData): Try[CommandHandlerResponse]
}

trait EventHandler {
  def handle(event: Any, currentState: Any, metaData: MetaData): Any
}

/**
 * LagompbCommandHandler
 *
 * @param actorSystem the actor system
 * @tparam S the aggregate state type
 */
abstract class SimpleCommandHandler[S <: scalapb.GeneratedMessage](actorSystem: ActorSystem,
                                                                   protosRegistry: ProtosRegistry
) extends CommandHandler {

  final def handle(command: Any, currentState: Any, currentMetaData: MetaData): Try[CommandHandlerResponse] = {
    protosRegistry.unpackAnys(currentState, command) match {
      case Failure(exception) =>
        throw exception

      case Success(messages) =>
        val stateUnpacked: S = messages(0).asInstanceOf[S]
        val cmdUnpacked = messages(1)
        handleTyped(command = cmdUnpacked, currentState = stateUnpacked, currentMetaData = currentMetaData)
    }
  }

  /**
   * Handles a given command send to the entity
   *
   * @param command   the actual to handle
   * @param currentState     the current aggregate state before the command was triggered
   * @param currentMetaData the current event meta before the command was triggered
   * @return CommandHandlerResponse
   */
  def handleTyped(command: scalapb.GeneratedMessage, currentState: S, currentMetaData: MetaData): Try[CommandHandlerResponse]
}

/**
 * LagomPbEventHandler
 *
 * @param actorSystem the actor system
 * @tparam S the aggregate state type
 */
abstract class SimpleEventHandler[S <: scalapb.GeneratedMessage](actorSystem: ActorSystem,
                                                                 protosRegistry: ProtosRegistry
) extends EventHandler {

  /**
   * uses protosRegistry to unmarshal proto messages and invoke implemented handleTyped
   *
   * @param event an Any message with an event
   * @param currentState an Any message with current state
   * @param metaData lagomPb MetaData
   * @return an Any message with a resulting state
   */
  final def handle(event: Any, currentState: Any, metaData: MetaData): Any = {
    protosRegistry.unpackAnys(currentState, event) match {
      case Failure(exception) =>
        throw exception

      case Success(messages) =>
        val stateUnpacked: S = messages(0).asInstanceOf[S]
        val eventUnpacked = messages(1)
        Any.pack(handleTyped(eventUnpacked, stateUnpacked, metaData))
    }
  }

  /**
   * Handles a given event ad return the resulting state
   *
   * @param event the event to handle
   * @param currentState the current state
   * @param metaData the event meta characterising the actual event.
   * @return the resulting state
   */
  def handleTyped(event: scalapb.GeneratedMessage, currentState: S, metaData: MetaData): S
}
