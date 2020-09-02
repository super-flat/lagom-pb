package io.superflat.lagompb

import akka.actor.ActorSystem
import com.google.protobuf.any.Any
import io.superflat.lagompb.protobuf.v1.core.{CommandHandlerResponse, MetaData}

import scala.util.{Failure, Success, Try}

/**
 * CommandHandler is a generic command handler
 */
trait CommandHandler {

  /**
   * generic method signature for handling commands
   *
   * @param command an Any message with a command inside
   * @param currentState an Any message with a State inside
   * @param currentMetaData lagom-pb meta data
   * @return a command handler response or Failure
   */
  def handle(command: Any, currentState: Any, currentMetaData: MetaData): Try[CommandHandlerResponse]
}

/**
 * EventHandler is a generic event handler
 */
trait EventHandler {

  /**
   * generic method signature for handling events
   *
   * @param event an Any message with an event inside
   * @param currentState an Any message with a State inside
   * @param metaData lagom-pb meta data
   * @return an Any message with the resulting state
   */
  def handle(event: Any, currentState: Any, metaData: MetaData): Any
}

/**
 * TypedCommandHandler is a typed command handler.
 * It makes use the protos registry to properly unpack the actual user command and state into a scalapb.GeneratedMessage
 * that can be easily pattern match
 *
 * @param actorSystem the actor system
 * @tparam S the aggregate state type
 */
abstract class TypedCommandHandler[S <: scalapb.GeneratedMessage](actorSystem: ActorSystem) extends CommandHandler {

  /**
   * implements CommandHandler.handle and uses proto registry to unmarshal
   * proto messages and invoke implemented handleTyped
   *
   * @param command an Any message with a command
   * @param currentState an Any message with current state
   * @param currentMetaData lagomPb MetaData
   * @return a command handler response or Failure
   */
  final def handle(command: Any, currentState: Any, currentMetaData: MetaData): Try[CommandHandlerResponse] = {
    ProtosRegistry.unpackAnys(currentState, command) match {
      case Failure(exception) =>
        Failure(exception)

      case Success(messages) =>
        handleTyped(
          command = messages.lift(1).head,
          currentState = messages.head.asInstanceOf[S],
          currentMetaData = currentMetaData
        )
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
  def handleTyped(command: scalapb.GeneratedMessage,
                  currentState: S,
                  currentMetaData: MetaData
  ): Try[CommandHandlerResponse]
}

/**
 * TypedEventHandler is a typed event handler. It makes use the protos registry
 * to properly unpack the actual user event and state into a scalapb.GeneratedMessage that can be
 * easily pattern match
 *
 * @param actorSystem the actor system
 * @tparam S the aggregate state type
 */
abstract class TypedEventHandler[S <: scalapb.GeneratedMessage](actorSystem: ActorSystem) extends EventHandler {

  /**
   * uses protosRegistry to unmarshal proto messages and invoke implemented handleTyped
   *
   * @param event an Any message with an event
   * @param currentState an Any message with current state
   * @param metaData lagomPb MetaData
   * @return an Any message with a resulting state
   */
  final def handle(event: Any, currentState: Any, metaData: MetaData): Any = {
    ProtosRegistry.unpackAnys(currentState, event) match {
      case Failure(exception) =>
        throw exception

      case Success(messages) =>
        Any.pack(
          handleTyped(
            messages.lift(1).head,
            messages.head.asInstanceOf[S],
            metaData
          )
        )
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
