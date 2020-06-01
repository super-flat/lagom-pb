package lagompb

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorRef
import akka.persistence.typed.PersistenceId
import com.google.protobuf.any.Any
import com.typesafe.config.{Config, ConfigFactory}
import lagompb.data.{TestAggregate, TestCommandHandler, TestEventHandler}
import lagompb.protobuf.core.CommandReply.Reply
import lagompb.protobuf.core._
import lagompb.protobuf.tests._
import lagompb.testkit.LagompbActorTestKit

import scala.concurrent.duration.FiniteDuration

class LagompbAggregateSpec extends LagompbActorTestKit(s"""
      akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      akka.persistence.snapshot-store.local.dir = "tmp/snapshot"
    """) {

  private val companyUUID = "93cfb5fc-c01b-4cda-bb45-31875bafda23"
  private val replyTimeout = FiniteDuration(30, TimeUnit.SECONDS)
  private val config: Config = ConfigFactory.load()

  private def randomId(): String = UUID.randomUUID().toString

  private def parseState(any: Any): TestState = {
    val typeUrl: String =
      any.typeUrl.substring(any.typeUrl.lastIndexOf('/') + 1)
    typeUrl match {
      case "lagompb.protobuf.TestState" =>
        TestState.parseFrom(any.value.toByteArray)
      case _ => throw new RuntimeException(s"wrong state definition $typeUrl")
    }
  }

  "LagompbAggregate Implementation" should {
    "handle command as expected" in {
      // Let us create the sender of commands
      val commandSender: TestProbe[CommandReply] =
        createTestProbe[CommandReply]()

      val aggregate = new TestAggregate(null, config, new TestCommandHandler(null), new TestEventHandler(null))

      // Let us create the aggregate
      val aggregateId: String = randomId()
      val aggregateRef: ActorRef[LagompbCommand] =
        spawn(aggregate.create(PersistenceId("TestAggregate", aggregateId)))
      val testCmd = TestCmd(companyUUID, "first test")

      // let us send the command to the aggregate
      val data =
        Map("audit|employeeUuid" -> "1223", "audit|createdAt" -> "2020-04-17")
      aggregateRef ! LagompbCommand(testCmd, commandSender.ref, data)
      commandSender.receiveMessage(replyTimeout) match {
        case CommandReply(reply) =>
          reply match {
            case Reply.Empty => fail("unexpected message state")
            case Reply.SuccessfulReply(value) =>
              val state = parseState(value.getStateWrapper.getState)
              state shouldBe TestState(companyUUID, "first test")
              value.getStateWrapper.getMeta.data shouldBe data
            case Reply.FailedReply(_) => fail("unexpected message state")
          }

        case _ => fail("unexpected message type")
      }
    }
    "handle invalid command as expected" in {
      // Let us create the sender of commands
      val commandSender: TestProbe[CommandReply] =
        createTestProbe[CommandReply]()

      val aggregate = new TestAggregate(null, config, new TestCommandHandler(null), new TestEventHandler(null))

      // Let us create the aggregate
      val aggregateId: String = randomId()
      val aggregateRef: ActorRef[LagompbCommand] =
        spawn(aggregate.create(PersistenceId("TestAggregate", aggregateId)))
      val testCmd = TestCmd("", "first test")

      // let us send the command to the aggregate
      aggregateRef ! LagompbCommand(testCmd, commandSender.ref, Map.empty[String, String])

      commandSender.receiveMessage(replyTimeout) match {
        case CommandReply(reply) =>
          reply match {
            case Reply.FailedReply(value) =>
              value.reason shouldBe "command is invalid"
            case _ => fail("unexpected message type")
          }
        case _ => fail("unexpected message type")
      }
    }
    "received command and persist no event" in {
      // Let us create the sender of commands
      val commandSender: TestProbe[CommandReply] =
        createTestProbe[CommandReply]()

      val aggregate = new TestAggregate(null, config, new TestCommandHandler(null), new TestEventHandler(null))

      // Let us create the aggregate
      val aggregateId: String = randomId()
      val aggregateRef: ActorRef[LagompbCommand] =
        spawn(aggregate.create(PersistenceId("TestAggregate", aggregateId)))
      val testCmd = TestGetCmd().withCompanyUuid(companyUUID)

      // let us send the command to the aggregate
      val data =
        Map("audit|employeeUuid" -> "1223", "audit|createdAt" -> "2020-04-17")
      aggregateRef ! LagompbCommand(testCmd, commandSender.ref, data)
      commandSender.receiveMessage(replyTimeout) match {
        case CommandReply(reply) =>
          reply match {
            case Reply.Empty => fail("unexpected message state")
            case Reply.SuccessfulReply(value) =>
              val state = parseState(value.getStateWrapper.getState)
              state shouldBe TestState.defaultInstance
              value.getStateWrapper.getMeta.data shouldBe empty
            case Reply.FailedReply(_) => fail("unexpected message state")
          }

        case _ => fail("unexpected message type")
      }
    }
    "received command and return unhandled response" in {
      // Let us create the sender of commands
      val commandSender: TestProbe[CommandReply] =
        createTestProbe[CommandReply]()

      val aggregate = new TestAggregate(null, config, new TestCommandHandler(null), new TestEventHandler(null))

      // Let us create the aggregate
      val aggregateId: String = randomId()
      val aggregateRef: ActorRef[LagompbCommand] =
        spawn(aggregate.create(PersistenceId("TestAggregate", aggregateId)))
      val testCmd = TestEmptyCmd().withCompanyUuid(companyUUID)

      // let us send the command to the aggregate
      aggregateRef ! LagompbCommand(testCmd, commandSender.ref, Map.empty)
      commandSender.receiveMessage(replyTimeout) match {
        case CommandReply(reply) =>
          reply match {
            case Reply.FailedReply(value) =>
              value.reason should include("unknown command handler response")
            case _ => fail("unexpected message type")
          }
        case _ => fail("unexpected message type")
      }
    }
    "received command and return unhandled success response" in {
      // Let us create the sender of commands
      val commandSender: TestProbe[CommandReply] =
        createTestProbe[CommandReply]()

      val aggregate = new TestAggregate(null, config, new TestCommandHandler(null), new TestEventHandler(null))

      // Let us create the aggregate
      val aggregateId: String = randomId()
      val aggregateRef: ActorRef[LagompbCommand] =
        spawn(aggregate.create(PersistenceId("TestAggregate", aggregateId)))
      val testCmd = TestEmptySuccessCmd().withCompanyUuid(companyUUID)

      // let us send the command to the aggregate
      aggregateRef ! LagompbCommand(testCmd, commandSender.ref, Map.empty)
      commandSender.receiveMessage(replyTimeout) match {
        case CommandReply(reply) =>
          reply match {
            case Reply.FailedReply(value) =>
              value.reason should include("unknown command handler success response")
            case _ => fail("unexpected message type")
          }
        case _ => fail("unexpected message type")
      }
    }

    "receive command and return an event out of scope" in {
      // Let us create the sender of commands
      val commandSender: TestProbe[CommandReply] =
        createTestProbe[CommandReply]()

      val aggregate = new TestAggregate(null, config, new TestCommandHandler(null), new TestEventHandler(null))

      // Let us create the aggregate
      val aggregateId: String = randomId()
      val aggregateRef: ActorRef[LagompbCommand] =
        spawn(aggregate.create(PersistenceId("TestAggregate", aggregateId)))
      val testCmd = TestUnknownEventCmd().withCompanyUuid(companyUUID)

      // let us send the command to the aggregate
      aggregateRef ! LagompbCommand(testCmd, commandSender.ref, Map.empty)
      commandSender.receiveMessage(replyTimeout) match {
        case CommandReply(reply) =>
          reply match {
            case Reply.FailedReply(value) =>
              value.reason should include("[Lagompb] unable to parse event")
              value.cause should ===(FailureCause.InternalError)
            case _ => fail("unexpected message type")
          }
        case _ => fail("unexpected message type")
      }
    }

    "handle command handler failure" in {
      // Let us create the sender of commands
      val commandSender: TestProbe[CommandReply] =
        createTestProbe[CommandReply]()

      val aggregate = new TestAggregate(null, config, new TestCommandHandler(null), new TestEventHandler(null))

      // Let us create the aggregate
      val aggregateId: String = randomId()
      val aggregateRef: ActorRef[LagompbCommand] =
        spawn(aggregate.create(PersistenceId("TestAggregate", aggregateId)))
      val testCmd = TestFailCmd().withCompanyUuid(companyUUID)

      // let us send the command to the aggregate
      aggregateRef ! LagompbCommand(testCmd, commandSender.ref, Map.empty)
      commandSender.expectNoMessage()
    }

    "handle wrong state parsing" in {
      val aggregate = new TestAggregate(null, config, new TestCommandHandler(null), new TestEventHandler(null))
      val stateWrapper = StateWrapper().withState(
        Any()
          .withTypeUrl("type.googleapis.com/lagom.test")
          .withValue(com.google.protobuf.ByteString.copyFrom("".getBytes))
      )

      an[LagompbException] shouldBe thrownBy(aggregate.genericCommandHandler(stateWrapper, null))
    }

    "handle generic event handler" in {
      val companyUuid = "12234"
      val aggregate = new TestAggregate(null, config, new TestCommandHandler(null), new TestEventHandler(null))
      val stateWrapper = StateWrapper()
        .withState(
          Any
            .pack(
              TestState()
                .withName("prior")
                .withCompanyUuid(companyUuid)
            )
        )
        .withMeta(MetaData.defaultInstance)

      val eventWrapper = EventWrapper()
        .withEvent(
          Any
            .pack(
              TestEvent()
                .withName("event")
                .withEventUuid("23")
            )
        )
        .withMeta(MetaData.defaultInstance)
        .withResultingState(
          Any
            .pack(
              TestState()
                .withName("resulting")
                .withCompanyUuid(companyUuid)
            )
        )

      val newState: StateWrapper =
        aggregate.genericEventHandler(stateWrapper, eventWrapper)
      newState.getState shouldBe (Any.pack(
        TestState()
          .withName("resulting")
          .withCompanyUuid(companyUuid)
      ))
    }

    "generic event handler handles wrong event" in {

      val aggregate = new TestAggregate(null, config, new TestCommandHandler(null), new TestEventHandler(null))
      an[LagompbException] shouldBe thrownBy(aggregate.genericEventHandler(null, WrongEventTagger.defaultInstance))
    }
  }
}
