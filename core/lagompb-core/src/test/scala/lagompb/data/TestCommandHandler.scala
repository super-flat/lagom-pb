package lagompb.data

import akka.actor.ActorSystem
import com.google.protobuf.any.Any
import lagompb.LagompbCommand
import lagompb.LagompbCommandHandler
import lagompb.protobuf.core.CommandHandlerResponse.HandlerResponse
import lagompb.protobuf.core._
import lagompb.protobuf.tests._

import scala.util.Try

class TestCommandHandler(actorSystem: ActorSystem) extends LagompbCommandHandler[TestState](actorSystem) {

  def handleTestGetCmd(cmd: TestGetCmd, currentState: TestState): Try[CommandHandlerResponse] = {
    Try(
      CommandHandlerResponse()
        .withSuccessResponse(
          SuccessCommandHandlerResponse()
            .withNoEvent(com.google.protobuf.empty.Empty())
        )
    )
  }

  def handleTestCmd(cmd: TestCmd, state: TestState): Try[CommandHandlerResponse] = {
    if (cmd.companyUuid.isEmpty) {
      Try(
        CommandHandlerResponse()
          .withFailedResponse(
            FailedCommandHandlerResponse()
              .withReason("command is invalid")
              .withCause(FailureCause.ValidationError)
          )
      )
    } else {
      Try(
        CommandHandlerResponse()
          .withSuccessResponse(
            SuccessCommandHandlerResponse()
              .withEvent(Any.pack(TestEvent(cmd.companyUuid, cmd.name)))
          )
      )
    }
  }

  def handleInvalidCommand(): Try[CommandHandlerResponse] =
    Try(
      CommandHandlerResponse()
        .withFailedResponse(
          FailedCommandHandlerResponse()
            .withReason("no such command")
            .withCause(FailureCause.InternalError)
        )
    )

  override def handle(
      command: LagompbCommand,
      currentState: TestState,
      currentEventMeta: MetaData
  ): Try[CommandHandlerResponse] = {
    command.command match {
      case cmd: TestCmd => handleTestCmd(cmd, currentState)
      case cmd: TestGetCmd => handleTestGetCmd(cmd, currentState)
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
      case _ => handleInvalidCommand()
    }
  }

}
