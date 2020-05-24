package lagompb

import java.util.UUID

import com.google.protobuf.any.Any
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import lagompb.data._
import lagompb.protobuf.core.CommandReply.Reply
import lagompb.protobuf.core._
import lagompb.protobuf.tests.TestCmd
import lagompb.protobuf.tests.TestState
import lagompb.testkit.LagompbSpec

import scala.concurrent.ExecutionContext.Implicits.global

class LagompbServiceImplSpec extends LagompbSpec {
  val companyId: String = UUID.randomUUID().toString
  val config: Config = ConfigFactory.load()
  val any: Any = Any()
    .withTypeUrl("type.googleapis.com/lagompb.protobuf.TestState")
    .withValue(
      TestState()
        .withCompanyUuid(companyId)
        .withName("test")
        .toByteString
    )

  val embeddedPostgres: EmbeddedPostgres.Builder = EmbeddedPostgres.builder()

  protected override def beforeAll(): Unit = {
    embeddedPostgres.start()
  }

  protected override def afterAll(): Unit = {}

  "LagompbService implementation" should {
    "parse proto Any and return a State" in {
      val commandHandler = new TestCommandHandler(null)
      val eventHandler = new TestEventHandler(null)
      val aggregate = new TestAggregate(null, config, commandHandler, eventHandler)
      val testImpl = new TestServiceImpl(null, null, null, aggregate)
      testImpl.parseAny[TestState](any) shouldBe
        TestState()
          .withCompanyUuid(companyId)
          .withName("test")
    }

    "fail to handle wrong proto Any" in {
      val commandHandler = new TestCommandHandler(null)
      val eventHandler = new TestEventHandler(null)
      val aggregate = new TestAggregate(null, config, commandHandler, eventHandler)
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
      val aggregate = new TestAggregate(null, config, commandHandler, eventHandler)
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

      val result: LagompbState[TestState] = testImpl.handleLagomPbCommandReply[TestState](cmdReply)

      result.state shouldBe
        TestState()
          .withCompanyUuid(companyId)
          .withName("test")
    }

    "handle FailedReply" in {
      val commandHandler = new TestCommandHandler(null)
      val eventHandler = new TestEventHandler(null)
      val aggregate = new TestAggregate(null, config, commandHandler, eventHandler)
      val testImpl = new TestServiceImpl(null, null, null, aggregate)
      val rejected =
        CommandReply()
          .withFailedReply(
            FailedReply()
              .withReason("failed")
          )
      an[RuntimeException] shouldBe thrownBy(testImpl.handleLagomPbCommandReply[TestState](rejected))
    }

    "failed to handle CommandReply" in {
      val commandHandler = new TestCommandHandler(null)
      val eventHandler = new TestEventHandler(null)
      val aggregate = new TestAggregate(null, config, commandHandler, eventHandler)
      val testImpl = new TestServiceImpl(null, null, null, aggregate)
      case class WrongReply()
      an[RuntimeException] shouldBe thrownBy(
        testImpl.handleLagomPbCommandReply[TestState](CommandReply().withReply(Reply.Empty))
      )
    }

    "parse State" in {
      val commandHandler = new TestCommandHandler(null)
      val eventHandler = new TestEventHandler(null)
      val aggregate = new TestAggregate(null, config, commandHandler, eventHandler)
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
      val aggregate = new TestAggregate(null, config, commandHandler, eventHandler)
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
