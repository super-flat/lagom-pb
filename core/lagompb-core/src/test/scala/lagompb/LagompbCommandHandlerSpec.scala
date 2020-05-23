package lagompb

import java.util.UUID

import com.google.protobuf.any.Any
import lagompb.protobuf.core._
import lagompb.data.TestCommandHandler
import lagompb.testkit.LagompbSpec
import lagompb.protobuf.tests.NoCmd
import lagompb.protobuf.tests.TestCmd
import lagompb.protobuf.tests.TestEvent
import lagompb.protobuf.tests.TestState

import scala.util.Try

class LagompbCommandHandlerSpec extends LagompbSpec {

  val companyId: String = UUID.randomUUID().toString
  val cmdHandler = new TestCommandHandler(null)

  "LagompbCommandHandler implementation" should {
    "handle valid command as expected" in {
      val testCmd = TestCmd(companyId, "test")
      val state = TestState(companyId, "state")

      val result: Try[CommandHandlerResponse] = cmdHandler.handleTestCmd(testCmd, state)
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
      val result: Try[CommandHandlerResponse] = cmdHandler.handleTestCmd(testCmd, state)

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
        cmdHandler.handle(LagompbCommand(testCmd, null, Map.empty[String, String]), state, meta)

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
        cmdHandler.handle(LagompbCommand(testCmd, null, Map.empty[String, String]), state, meta)

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

    "handle no such NamelyCommand" in {
      val noCmd = NoCmd()
      val meta = MetaData(revisionNumber = 1)
      cmdHandler
        .handle(LagompbCommand(noCmd, null, Map.empty[String, String]), TestState(companyId, "state"), meta)
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
