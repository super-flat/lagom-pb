package io.superflat.lagompb.data

import akka.actor.ActorSystem
import com.google.protobuf.any.Any
import io.superflat.lagompb.protobuf.v1.core._
import io.superflat.lagompb.protobuf.v1.core.CommandHandlerResponse.HandlerResponse
import io.superflat.lagompb.protobuf.v1.tests._
import io.superflat.lagompb.{ProtosRegistry, TypedCommandHandler}

import scala.util.Try

class TestCommandHandler(actorSystem: ActorSystem) extends TypedCommandHandler[TestState](actorSystem) {

  override def handleTyped(
    command: scalapb.GeneratedMessage,
    currentState: TestState,
    currentEventMeta: MetaData
  ): Try[CommandHandlerResponse] =
    command match {
      case cmd: TestCmd             => handleTestCmd(cmd, currentState)
      case cmd: TestGetCmd          => handleTestGetCmd(cmd, currentState)
      case cmd: TestEventFailureCmd => handleTestEventHandlerFailure(cmd, currentState)
      case _: TestEmptyCmd =>
        Try(
          CommandHandlerResponse()
            .withHandlerResponse(HandlerResponse.Empty)
        )
      case _: TestEmptySuccessCmd =>
        Try(
          CommandHandlerResponse()
            .withSuccessResponse(SuccessCommandHandlerResponse.defaultInstance)
        )
      case _: TestUnknownEventCmd =>
        Try(
          CommandHandlerResponse()
            .withSuccessResponse(
              SuccessCommandHandlerResponse()
                .withEvent(
                  Any()
                    .withTypeUrl("type.googleapis.com/lagom.test")
                    .withValue(com.google.protobuf.ByteString.copyFrom("".getBytes))
                )
            )
        )
      case _: TestFailCmd => throw new RuntimeException("I am failing...")
      case _              => handleInvalidCommand()
    }

  def handleTestEventHandlerFailure(cmd: TestEventFailureCmd, currentState: TestState): Try[CommandHandlerResponse] =
    Try(
      CommandHandlerResponse()
        .withSuccessResponse(
          SuccessCommandHandlerResponse()
            .withEvent(Any.pack(TestEventFailure.defaultInstance))
        )
    )

  def handleTestGetCmd(cmd: TestGetCmd, currentState: TestState): Try[CommandHandlerResponse] =
    Try(
      CommandHandlerResponse()
        .withSuccessResponse(
          SuccessCommandHandlerResponse()
            .withNoEvent(com.google.protobuf.empty.Empty())
        )
    )

  def handleTestCmd(cmd: TestCmd, state: TestState): Try[CommandHandlerResponse] =
    if (cmd.companyUuid.isEmpty)
      Try(
        CommandHandlerResponse()
          .withFailedResponse(
            FailedCommandHandlerResponse()
              .withReason("command is invalid")
              .withCause(FailureCause.VALIDATION_ERROR)
          )
      )
    else
      Try(
        CommandHandlerResponse()
          .withSuccessResponse(
            SuccessCommandHandlerResponse()
              .withEvent(Any.pack(TestEvent(cmd.companyUuid, cmd.name)))
          )
      )

  def handleInvalidCommand(): Try[CommandHandlerResponse] =
    Try(
      CommandHandlerResponse()
        .withFailedResponse(
          FailedCommandHandlerResponse()
            .withReason("no such command")
            .withCause(FailureCause.INTERNAL_ERROR)
        )
    )

}
