package io.superflat.lagompb

import java.util.UUID

import com.google.protobuf.any.Any
import io.superflat.lagompb.data.TestCommandHandler
import io.superflat.lagompb.protobuf.core._
import io.superflat.lagompb.protobuf.tests.{NoCmd, TestCmd, TestEvent, TestState}
import io.superflat.lagompb.testkit.LagompbSpec

import scala.util.Try

class CommandHandlerSpec extends LagompbSpec {

  val companyId: String = UUID.randomUUID().toString
  val cmdHandler = new TestCommandHandler(null)

  "CommandHandler implementation" should {
    "handle valid command as expected" in {
      val testCmd = TestCmd(companyId, "test")
      val state = TestState(companyId, "state")

      val result: Try[CommandHandlerResponse] =
        cmdHandler.handleTestCmd(testCmd, state)
      result.success.value shouldBe
        CommandHandlerResponse()
          .withSuccessResponse(
            SuccessCommandHandlerResponse()
              .withEvent(Any.pack(TestEvent(companyId, "test")))
          )
    }

    "handle invalid command as expected" in {
      val testCmd = TestCmd("", "test")
      val state = TestState(UUID.randomUUID().toString, "state")
      val result: Try[CommandHandlerResponse] =
        cmdHandler.handleTestCmd(testCmd, state)

      result.success.value shouldBe
        CommandHandlerResponse()
          .withFailedResponse(
            FailedCommandHandlerResponse()
              .withReason("command is invalid")
              .withCause(FailureCause.ValidationError)
          )
    }

    "handle BaseCommand as expected" in {
      val testCmd = TestCmd(companyId, "test")
      val state = TestState(companyId, "state")
      val meta = MetaData(revisionNumber = 1)
      val result: Try[CommandHandlerResponse] =
        cmdHandler.handle(Command(testCmd, null, Map.empty[String, String]), state, meta)

      result.success.value shouldBe
        CommandHandlerResponse()
          .withSuccessResponse(
            SuccessCommandHandlerResponse()
              .withEvent(Any.pack(TestEvent(companyId, "test")))
          )
    }

    "handle invalid BaseCommand as expected" in {
      val testCmd = TestCmd("", "test")
      val state = TestState(UUID.randomUUID().toString, "state")
      val meta = MetaData(revisionNumber = 1)
      val result: Try[CommandHandlerResponse] =
        cmdHandler.handle(Command(testCmd, null, Map.empty[String, String]), state, meta)

      result.success.value shouldBe
        CommandHandlerResponse()
          .withFailedResponse(
            FailedCommandHandlerResponse()
              .withReason("command is invalid")
              .withCause(FailureCause.ValidationError)
          )
    }

    "handle no such command" in {
      cmdHandler.handleInvalidCommand().success.value shouldBe
        CommandHandlerResponse()
          .withFailedResponse(
            FailedCommandHandlerResponse()
              .withReason("no such command")
              .withCause(FailureCause.InternalError)
          )
    }

    "handle no such LagompbCommand" in {
      val noCmd = NoCmd()
      val meta = MetaData(revisionNumber = 1)
      cmdHandler
        .handle(Command(noCmd, null, Map.empty[String, String]), TestState(companyId, "state"), meta)
        .success
        .value shouldBe
        CommandHandlerResponse()
          .withFailedResponse(
            FailedCommandHandlerResponse()
              .withReason("no such command")
              .withCause(FailureCause.InternalError)
          )
    }
  }
}
