package io.superflat.lagompb.data

import akka.actor.ActorSystem
import com.google.protobuf.any.Any
import io.superflat.lagompb.TypedCommandHandler
import io.superflat.lagompb.protobuf.v1.core._
import io.superflat.lagompb.protobuf.v1.tests._

import scala.util.{Failure, Try}

class TestCommandHandler(actorSystem: ActorSystem) extends TypedCommandHandler[TestState](actorSystem) {

  override def handleTyped(
      command: scalapb.GeneratedMessage,
      currentState: TestState,
      currentEventMeta: MetaData
  ): Try[CommandHandlerResponse] =
    command match {
      case cmd: TestCommand                          => handleTestCommand(cmd, currentState)
      case cmd: NoEventTestCommand                   => handleNoEventTestCommand(cmd, currentState)
      case cmd: CriticalFailureTestCommand           => handleCriticalFailureTestCommand(cmd, currentState)
      case cmd: NotFoundFailureTestCommand           => handleNotFoundFailureTestCommand(cmd, currentState)
      case cmd: CustomFailureTestCommand             => handleCustomFailureTestCommand(cmd, currentState)
      case _: EmptyCommandHandlerResponseTestCommand => Try(CommandHandlerResponse())
      case _: UnknownTestEventCommand                => handleUnknownEventTestCommand()
      case _                                         => Failure(new RuntimeException("unknown"))
    }

  def handleNotFoundFailureTestCommand(
      cmd: NotFoundFailureTestCommand,
      currentState: TestState
  ): Try[CommandHandlerResponse] = {
    Try(CommandHandlerResponse().withFailure(FailureResponse().withNotFound("Oops!!!")))
  }

  def handleCustomFailureTestCommand(
      cmd: CustomFailureTestCommand,
      currentState: TestState
  ): Try[CommandHandlerResponse] = {
    Try(CommandHandlerResponse().withFailure(FailureResponse().withCustom(Any.pack(com.google.protobuf.empty.Empty()))))
  }

  def handleUnknownEventTestCommand(): Try[CommandHandlerResponse] = {
    Try(
      CommandHandlerResponse().withEvent(
        Any()
          .withTypeUrl("type.googleapis.com/lagom.test")
          .withValue(com.google.protobuf.ByteString.copyFrom("".getBytes))
      )
    )
  }

  def handleCriticalFailureTestCommand(
      cmd: CriticalFailureTestCommand,
      currentState: TestState
  ): Try[CommandHandlerResponse] =
    Try(CommandHandlerResponse().withFailure(FailureResponse().withCritical("Oops!!!")))

  def handleNoEventTestCommand(cmd: NoEventTestCommand, currentState: TestState): Try[CommandHandlerResponse] =
    Try(CommandHandlerResponse().withNoEvent(com.google.protobuf.empty.Empty()))

  def handleTestCommand(cmd: TestCommand, state: TestState): Try[CommandHandlerResponse] =
    if (cmd.companyUuid.isEmpty)
      Try(CommandHandlerResponse().withFailure(FailureResponse().withValidation("command is invalid")))
    else
      Try(CommandHandlerResponse().withEvent(Any.pack(TestEvent(cmd.companyUuid, cmd.name))))

}
