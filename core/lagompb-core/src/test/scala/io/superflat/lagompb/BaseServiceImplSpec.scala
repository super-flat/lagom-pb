/*
 * Copyright (C) 2020 Superflat. <https://github.com/super-flat>
 */

package io.superflat.lagompb

import akka.actor.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import com.google.protobuf.any.Any
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import io.superflat.lagompb.data._
import io.superflat.lagompb.encryption.EncryptionAdapter
import io.superflat.lagompb.protobuf.v1.core._
import io.superflat.lagompb.protobuf.v1.core.CommandReply.Reply
import io.superflat.lagompb.protobuf.v1.tests.{TestCommand, TestState}
import io.superflat.lagompb.testkit.BaseSpec
import org.testcontainers.utility.DockerImageName

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class BaseServiceImplSpec extends BaseSpec with ForAllTestContainer {

  override val container: PostgreSQLContainer = PostgreSQLContainer
    .Def(
      dockerImageName = DockerImageName.parse("postgres"),
      databaseName = "postgres",
      username = "postgres",
      password = "postgres",
      urlParams = Map("currentSchema" -> "public")
    )
    .createContainer()

  val companyId: String = UUID.randomUUID().toString

  val any: Any =
    Any.pack(
      TestState()
        .withCompanyUuid(companyId)
        .withName("test")
    )

  val defaultEncryptionAdapter = new EncryptionAdapter(None)
  var actorSystem: ActorSystem = _
  var clusterSharding: ClusterSharding = _
  var peristenceRegistry: PersistentEntityRegistry = _

  val commandHandler = new TestCommandHandler(actorSystem)
  val eventHandler = new TestEventHandler(actorSystem)
  val aggregate =
    new TestAggregateRoot(actorSystem, commandHandler, eventHandler, TestState(), defaultEncryptionAdapter)
  val testImpl = new TestServiceImpl(actorSystem, clusterSharding, peristenceRegistry, aggregate)

  override protected def afterAll(): Unit = {
    super.afterAll()
  }

  "Service implementation" should {

    "handle SuccessfulReply" in {
      val cmdReply = CommandReply().withStateWrapper(
        StateWrapper()
          .withState(any)
          .withMeta(MetaData().withRevisionNumber(1))
      )

      val result: StateWrapper = testImpl.handleLagompbCommandReply(cmdReply).success.value

      val expectedState = TestState()
        .withCompanyUuid(companyId)
        .withName("test")

      result.state shouldBe Some(Any.pack(expectedState))
    }

    "handle FailedReply" in {
      val rejected =
        CommandReply().withFailure(FailureResponse().withCritical("failed"))
      testImpl.handleLagompbCommandReply(rejected).failure.exception shouldBe an[RuntimeException]
    }

    "failed to handle CommandReply" in {
      testImpl
        .handleLagompbCommandReply(CommandReply().withReply(Reply.Empty))
        .failure
        .exception shouldBe an[RuntimeException]
    }

    "process request as expected" in ServiceTest.withServer(ServiceTest.defaultSetup.withCluster()) { context =>
      new TestApplication(context)
    } { server =>
      val testCmd: TestCommand = TestCommand().withCompanyUuid(companyId).withName("John")
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
