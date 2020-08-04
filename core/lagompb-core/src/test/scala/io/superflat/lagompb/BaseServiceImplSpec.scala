package io.superflat.lagompb

import java.util.UUID

import com.google.protobuf.any.Any
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.superflat.lagompb.data._
import io.superflat.lagompb.encryption.EncryptionAdapter
import io.superflat.lagompb.protobuf.core._
import io.superflat.lagompb.protobuf.core.CommandReply.Reply
import io.superflat.lagompb.protobuf.tests.{TestCmd, TestState}
import io.superflat.lagompb.testkit.BaseSpec

import scala.concurrent.ExecutionContext.Implicits.global

class BaseServiceImplSpec extends BaseSpec {
  val companyId: String = UUID.randomUUID().toString

  val any: Any =
    Any.pack(
      TestState()
        .withCompanyUuid(companyId)
        .withName("test")
    )

  val embeddedPostgres: EmbeddedPostgres.Builder = EmbeddedPostgres.builder()

  val defaultEncryptionAdapter = new EncryptionAdapter(None)

  override protected def beforeAll(): Unit =
    embeddedPostgres.start()

  "Service implementation" should {
    "parse proto Any and return a State" in {
      val commandHandler = new TestCommandHandler(null)
      val eventHandler = new TestEventHandler(null)
      val aggregate =
        new TestAggregateRoot(null, commandHandler, eventHandler, defaultEncryptionAdapter)
      val testImpl = new TestServiceImpl(null, null, null, aggregate)
      testImpl.parseAny[TestState](any) shouldBe
        TestState()
          .withCompanyUuid(companyId)
          .withName("test")
    }

    "fail to handle wrong proto Any" in {
      val commandHandler = new TestCommandHandler(null)
      val eventHandler = new TestEventHandler(null)
      val aggregate =
        new TestAggregateRoot(null, commandHandler, eventHandler, defaultEncryptionAdapter)
      val testImpl = new TestServiceImpl(null, null, null, aggregate)
      an[RuntimeException] shouldBe thrownBy(
        testImpl.parseAny[TestState](
          Any()
            .withTypeUrl("type.googleapis.com/lagompb.test")
            .withValue(com.google.protobuf.ByteString.copyFrom("test".getBytes))
        )
      )
    }

    "handle SuccessfulReply" in {
      val commandHandler = new TestCommandHandler(null)
      val eventHandler = new TestEventHandler(null)
      val aggregate =
        new TestAggregateRoot(null, commandHandler, eventHandler, defaultEncryptionAdapter)
      val testImpl = new TestServiceImpl(null, null, null, aggregate)

      val cmdReply = CommandReply()
        .withSuccessfulReply(
          SuccessfulReply()
            .withStateWrapper(
              StateWrapper()
                .withState(any)
                .withMeta(MetaData().withRevisionNumber(1))
            )
        )

      val result: StateAndMeta[TestState] =
        testImpl.handleLagompbCommandReply[TestState](cmdReply)

      result.state shouldBe
        TestState()
          .withCompanyUuid(companyId)
          .withName("test")
    }

    "handle FailedReply" in {
      val commandHandler = new TestCommandHandler(null)
      val eventHandler = new TestEventHandler(null)
      val aggregate =
        new TestAggregateRoot(null, commandHandler, eventHandler, defaultEncryptionAdapter)
      val testImpl = new TestServiceImpl(null, null, null, aggregate)
      val rejected =
        CommandReply()
          .withFailedReply(
            FailedReply()
              .withReason("failed")
          )
      an[RuntimeException] shouldBe thrownBy(testImpl.handleLagompbCommandReply[TestState](rejected))
    }

    "failed to handle CommandReply" in {
      val commandHandler = new TestCommandHandler(null)
      val eventHandler = new TestEventHandler(null)
      val aggregate =
        new TestAggregateRoot(null, commandHandler, eventHandler, defaultEncryptionAdapter)
      val testImpl = new TestServiceImpl(null, null, null, aggregate)
      case class WrongReply()
      an[RuntimeException] shouldBe thrownBy(
        testImpl.handleLagompbCommandReply[TestState](CommandReply().withReply(Reply.Empty))
      )
    }

    "parse State" in {
      val commandHandler = new TestCommandHandler(null)
      val eventHandler = new TestEventHandler(null)
      val aggregate =
        new TestAggregateRoot(null, commandHandler, eventHandler, defaultEncryptionAdapter)
      val testImpl = new TestServiceImpl(null, null, null, aggregate)

      testImpl
        .parseState[TestState](
          StateWrapper()
            .withState(any)
            .withMeta(MetaData().withRevisionNumber(1))
        )
        .state shouldBe
        TestState()
          .withCompanyUuid(companyId)
          .withName("test")
    }

    "fail to parse state[No State provided]" in {
      val commandHandler = new TestCommandHandler(null)
      val eventHandler = new TestEventHandler(null)
      val aggregate =
        new TestAggregateRoot(null, commandHandler, eventHandler, defaultEncryptionAdapter)
      val testImpl = new TestServiceImpl(null, null, null, aggregate)
      an[RuntimeException] shouldBe thrownBy(
        testImpl.parseState[TestState](
          StateWrapper()
            .withState(
              Any()
                .withTypeUrl("type.googleapis.com/com.contonso.test")
                .withValue(com.google.protobuf.ByteString.copyFrom("test".getBytes))
            )
            .withMeta(MetaData().withRevisionNumber(1))
        )
      )
    }

    "process request as expected" in ServiceTest.withServer(ServiceTest.defaultSetup.withCluster()) { context =>
      new TestApplication(context)
    } { server =>
      val testCmd = TestCmd().withCompanyUuid(companyId).withName("John")
      val client: TestService = server.serviceClient.implement[TestService]
      client.testHello.invoke(testCmd).map { response: TestState =>
        response should ===(
          TestState()
            .withCompanyUuid(companyId)
            .withName("John")
        )
      }
    }
  }
}
