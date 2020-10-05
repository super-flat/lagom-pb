/*
 * Copyright (C) 2020 Superflat. <https://github.com/super-flat>
 */

package io.superflat.lagompb

import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import com.google.protobuf.any.Any
import io.superflat.lagompb.protobuf.v1.core.CommandReply.Reply
import io.superflat.lagompb.protobuf.v1.core._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait SendCommand {
  // $COVERAGE-OFF$
  implicit val timeout: Timeout = ConfigReader.askTimeout

  /**
   * Custom Command Error handler that needs to be implemented.
   */

  /**
   * sends commands to the aggregate root and return a future of the aggregate state given a entity Id
   * the given entity Id is obtained the cluster shard.
   *
   * @param clusterSharding  an instance of ClusterSharding
   * @param aggregateRoot    aggregateRoot defines the persistent entity that will be used to handle commands
   * @param entityId         the entity Id added or retrieved from the shard
   * @param cmd              the command to send to the aggregate. It is a scalapb generated case class from the command
   *                         protocol buffer message definition
   * @param data             additional data that need to be set in the state meta
   * @return Future of state
   */
  def sendCommand(
      clusterSharding: ClusterSharding,
      aggregateRoot: AggregateRoot,
      entityId: String,
      cmd: scalapb.GeneratedMessage,
      data: Map[String, Any]
  )(implicit ec: ExecutionContext): Future[StateWrapper] =
    clusterSharding
      .entityRefFor(aggregateRoot.typeKey, entityId)
      .ask[CommandReply](replyTo => Command(Any.pack(cmd), replyTo, data))
      .flatMap((value: CommandReply) => Future.fromTry(handleLagompbCommandReply(value)))

  /**
   * a "typed" version of send command that attempts to unpack the Any State
   * instance into the child GeneratedMessage using ProtosRegistry
   *
   * @param clusterSharding  an instance of ClusterSharding
   * @param aggregateRoot    aggregateRoot defines the persistent entity that will be used to handle commands
   * @param entityId         the entity Id added or retrieved from the shard
   * @param cmd              the command to send to the aggregate. It is a scalapb generated case class from the command
   *                         protocol buffer message definition
   * @param data             additional data that need to be set in the state meta
   * @param ec               some execution context
   * @return Future of state
   */
  def sendCommandTyped(
      clusterSharding: ClusterSharding,
      aggregateRoot: AggregateRoot,
      entityId: String,
      cmd: scalapb.GeneratedMessage,
      data: Map[String, Any]
  )(implicit
      ec: ExecutionContext
  ): Future[(scalapb.GeneratedMessage, MetaData)] =
    sendCommand(clusterSharding, aggregateRoot, entityId, cmd, data)
      .flatMap(stateWrapper => Future.fromTry(unpackStateWrapper(stateWrapper)))

  // $COVERAGE-ON$

  /**
   * generic handler for converting CommandReply into a StateWrapper
   *
   * @param commandReply some command handler reply
   * @return a state wrapper instance with state ane meta
   */
  private[lagompb] def handleLagompbCommandReply(
      commandReply: CommandReply
  ): Try[StateWrapper] = {
    commandReply.reply match {
      case Reply.Empty =>
        Failure(
          new RuntimeException(
            s"unknown CommandReply ${commandReply.reply.getClass.getName}"
          )
        )
      case Reply.StateWrapper(value: StateWrapper) => Success(value)
      case Reply.Failure(value: FailureResponse) =>
        transformFailedReply(value).asInstanceOf[Try[StateWrapper]]
    }
  }

  /**
   * generic conversion for failed replys into a scala Failure
   *
   * @param failureResponse some command handler failed reply
   * @return a Failure of type Try[]
   */
  def transformFailedReply(
      failureResponse: FailureResponse
  ): Failure[Throwable]

  /**
   * unpack state wrapper, for use in sendCommandTyped
   *
   * @param stateWrapper a state wrapper instance
   * @return a Try with the unpacked state as a generated message
   */
  def unpackStateWrapper(
      stateWrapper: StateWrapper
  ): Try[(scalapb.GeneratedMessage, MetaData)] =
    stateWrapper.state match {
      case Some(state) =>
        ProtosRegistry.unpackAny(state) match {
          case Failure(exception) => Failure(exception)
          case Success(newState)  => Success((newState, stateWrapper.getMeta))
        }
      case None => Failure(new RuntimeException("state not found"))
    }
}
