package io.superflat.lagompb

import java.util.UUID

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import com.google.protobuf.any.Any
import io.superflat.lagompb.data.TestCommandHandler
import io.superflat.lagompb.testkit.BaseActorTestKit
import io.superflat.lagompb.protobuf.v1.core._
import io.superflat.lagompb.protobuf.v1.tests.{NoCmd, TestCmd, TestEvent, TestState}

import scala.util.Try

class CommandHandlerSpec extends BaseActorTestKit(s"""
      akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      akka.persistence.snapshot-store.local.dir = "tmp/snapshot"
    """) {

  val actorSystem: ActorSystem = testKit.system.toClassic
  val companyId: String = UUID.randomUUID().toString
  val cmdHandler = new TestCommandHandler(actorSystem)

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
              .withCause(FailureCause.VALIDATION_ERROR)
          )
    }

    "handle BaseCommand as expected" in {
      val testCmd = TestCmd(companyId, "test")
      val state = TestState(companyId, "state")
      val meta = MetaData(revisionNumber = 1)
      val result: Try[CommandHandlerResponse] =
        cmdHandler.handle(Any.pack(testCmd), Any.pack(state), meta)

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
        cmdHandler.handle(Any.pack(testCmd), Any.pack(state), meta)

      result.success.value shouldBe
        CommandHandlerResponse()
          .withFailedResponse(
            FailedCommandHandlerResponse()
              .withReason("command is invalid")
              .withCause(FailureCause.VALIDATION_ERROR)
          )
    }

    "handle no such command" in {
      cmdHandler.handleInvalidCommand().success.value shouldBe
        CommandHandlerResponse()
          .withFailedResponse(
            FailedCommandHandlerResponse()
              .withReason("no such command")
              .withCause(FailureCause.INTERNAL_ERROR)
          )
    }

    "handle no such LagompbCommand" in {
      val noCmd = NoCmd()
      val meta = MetaData(revisionNumber = 1)
      cmdHandler
        .handle(Any.pack(noCmd), Any.pack(TestState(companyId, "state")), meta)
        .success
        .value shouldBe
        CommandHandlerResponse()
          .withFailedResponse(
            FailedCommandHandlerResponse()
              .withReason("no such command")
              .withCause(FailureCause.INTERNAL_ERROR)
          )
    }
  }
}
