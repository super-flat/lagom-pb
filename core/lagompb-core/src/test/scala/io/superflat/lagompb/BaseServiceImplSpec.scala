package io.superflat.lagompb

import java.util.UUID

import com.google.protobuf.any.Any
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import io.superflat.lagompb.data._
import io.superflat.lagompb.encryption.EncryptionAdapter
import io.superflat.lagompb.protobuf.v1.core.CommandReply.Reply
import io.superflat.lagompb.protobuf.v1.core._
import io.superflat.lagompb.protobuf.v1.tests.{TestCmd, TestState}
import io.superflat.lagompb.testkit.BaseSpec

import scala.concurrent.ExecutionContext.Implicits.global

class BaseServiceImplSpec extends BaseSpec {
  val companyId: String = UUID.randomUUID().toString
  val protosRegistry: ProtosRegistry = ProtosRegistry.fromReflection()

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
      val commandHandler = new TestCommandHandler(null, protosRegistry)
      val eventHandler = new TestEventHandler(null, protosRegistry)
      val aggregate =
        new TestAggregateRoot(null, commandHandler, eventHandler, defaultEncryptionAdapter)
      val testImpl = new TestServiceImpl(null, null, null, aggregate, protosRegistry)
      testImpl.parseAny[TestState](any) shouldBe
        TestState()
          .withCompanyUuid(companyId)
          .withName("test")
    }

    "fail to handle wrong proto Any" in {
      val commandHandler = new TestCommandHandler(null, protosRegistry)
      val eventHandler = new TestEventHandler(null, protosRegistry)
      val aggregate =
        new TestAggregateRoot(null, commandHandler, eventHandler, defaultEncryptionAdapter)
      val testImpl = new TestServiceImpl(null, null, null, aggregate, protosRegistry)
      an[RuntimeException] shouldBe thrownBy(
        testImpl.parseAny[TestState](
          Any()
            .withTypeUrl("type.googleapis.com/lagompb.test")
            .withValue(com.google.protobuf.ByteString.copyFrom("test".getBytes))
        )
      )
    }

    "handle SuccessfulReply" in {
      val commandHandler = new TestCommandHandler(null, protosRegistry)
      val eventHandler = new TestEventHandler(null, protosRegistry)
      val aggregate =
        new TestAggregateRoot(null, commandHandler, eventHandler, defaultEncryptionAdapter)
      val testImpl = new TestServiceImpl(null, null, null, aggregate, protosRegistry)

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
        testImpl.handleLagompbCommandReply[TestState](cmdReply).success.value

      result.state shouldBe
        TestState()
          .withCompanyUuid(companyId)
          .withName("test")
    }

    "handle FailedReply" in {
      val commandHandler = new TestCommandHandler(null, protosRegistry)
      val eventHandler = new TestEventHandler(null, protosRegistry)
      val aggregate =
        new TestAggregateRoot(null, commandHandler, eventHandler, defaultEncryptionAdapter)
      val testImpl = new TestServiceImpl(null, null, null, aggregate, protosRegistry)
      val rejected =
        CommandReply()
          .withFailedReply(
            FailedReply()
              .withReason("failed")
          )
      testImpl.handleLagompbCommandReply[TestState](rejected).failure.exception shouldBe an[RuntimeException]
    }

    "failed to handle CommandReply" in {
      val commandHandler = new TestCommandHandler(null, protosRegistry)
      val eventHandler = new TestEventHandler(null, protosRegistry)
      val aggregate =
        new TestAggregateRoot(null, commandHandler, eventHandler, defaultEncryptionAdapter)
      val testImpl = new TestServiceImpl(null, null, null, aggregate, protosRegistry)
      case class WrongReply()
      testImpl
        .handleLagompbCommandReply[TestState](CommandReply().withReply(Reply.Empty))
        .failure
        .exception shouldBe an[RuntimeException]
    }

    "parse State" in {
      val commandHandler = new TestCommandHandler(null, protosRegistry)
      val eventHandler = new TestEventHandler(null, protosRegistry)
      val aggregate =
        new TestAggregateRoot(null, commandHandler, eventHandler, defaultEncryptionAdapter)
      val testImpl = new TestServiceImpl(null, null, null, aggregate, protosRegistry)

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
      val commandHandler = new TestCommandHandler(null, protosRegistry)
      val eventHandler = new TestEventHandler(null, protosRegistry)
      val aggregate =
        new TestAggregateRoot(null, commandHandler, eventHandler, defaultEncryptionAdapter)
      val testImpl = new TestServiceImpl(null, null, null, aggregate, protosRegistry)
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
      val testCmd: TestCmd = TestCmd().withCompanyUuid(companyId).withName("John")
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
