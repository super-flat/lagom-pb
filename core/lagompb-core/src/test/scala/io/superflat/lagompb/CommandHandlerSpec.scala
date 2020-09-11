package io.superflat.lagompb

import java.util.UUID

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import com.google.protobuf.any.Any
import io.superflat.lagompb.data.TestCommandHandler
import io.superflat.lagompb.protobuf.v1.core.{CommandHandlerResponse, _}
import io.superflat.lagompb.protobuf.v1.tests._
import io.superflat.lagompb.testkit.BaseActorTestKit

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
      val testCmd = TestCommand(companyId, "test")
      val state = TestState(companyId, "state")

      val result: Try[CommandHandlerResponse] =
        cmdHandler.handleTyped(testCmd, state, MetaData.defaultInstance)
      result.success.value shouldBe
        CommandHandlerResponse().withEvent(Any.pack(TestEvent(companyId, "test")))
    }

    "handle invalid command as expected" in {
      val testCmd = TestCommand("", "test")
      val state = TestState(UUID.randomUUID().toString, "state")
      val result: Try[CommandHandlerResponse] =
        cmdHandler.handleTyped(testCmd, state, MetaData.defaultInstance)

      result.success.value shouldBe
        CommandHandlerResponse().withFailure(FailureResponse().withValidation("command is invalid"))
    }

    "handle BaseCommand as expected" in {
      val testCmd = TestCommand(companyId, "test")
      val state = TestState(companyId, "state")
      val meta = MetaData(revisionNumber = 1)
      val result: Try[CommandHandlerResponse] =
        cmdHandler.handleTyped(testCmd, state, meta)

      result.success.value shouldBe
        CommandHandlerResponse().withEvent(Any.pack(TestEvent(companyId, "test")))
    }

    "handle invalid BaseCommand as expected" in {
      val testCmd = TestCommand("", "test")
      val state = TestState(UUID.randomUUID().toString, "state")
      val meta = MetaData(revisionNumber = 1)
      val result: Try[CommandHandlerResponse] =
        cmdHandler.handleTyped(testCmd, state, meta)

      result.success.value shouldBe
        CommandHandlerResponse().withFailure(FailureResponse().withValidation("command is invalid"))
    }

    "handle command handler breakdown" in {
      val noCmd = NoCmd()
      val meta = MetaData(revisionNumber = 1)
      cmdHandler
        .handleTyped(noCmd, TestState(companyId, "state"), meta)
        .failure
        .exception
        .getMessage shouldBe "unknown"

    }

    "handled custom error" in {
      val testCmd = CustomFailureTestCommand.defaultInstance
      val state = TestState(UUID.randomUUID().toString, "state")
      val result: Try[CommandHandlerResponse] =
        cmdHandler.handleTyped(testCmd, state, MetaData.defaultInstance)

      val value = result.success.value.getFailure
      value.failureType shouldBe a[FailureResponse.FailureType.Custom]
    }
  }
}
