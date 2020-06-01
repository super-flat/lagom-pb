package lagompb.data

import java.util.UUID

import akka.actor.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.lightbend.lagom.scaladsl.api.Service.restCall
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, ServiceCall}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.lightbend.lagom.scaladsl.server.{LagomApplicationContext, LagomServer, LocalServiceLocator}
import com.softwaremill.macwire.wire
import lagompb.tests.{TestCmd, TestState}
import lagompb.{
  LagompbAggregate,
  LagompbApplication,
  LagompbCommandHandler,
  LagompbEventHandler,
  LagompbServiceImplWithKafka,
  LagompbServiceWithKafka,
  LagompbState
}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

import scala.concurrent.ExecutionContext

trait TestServiceWithkafka extends LagompbServiceWithKafka {
  def testHello: ServiceCall[TestCmd, TestState]

  override def routes: Seq[Descriptor.Call[_, _]] = Seq(restCall(Method.POST, "/api/hello", testHello _))
}

class TestServiceImplWithKafka(
    sys: ActorSystem,
    clusterSharding: ClusterSharding,
    persistentEntityRegistry: PersistentEntityRegistry,
    aggregate: LagompbAggregate[TestState]
)(implicit ec: ExecutionContext)
    extends LagompbServiceImplWithKafka(clusterSharding, persistentEntityRegistry, aggregate)
    with TestServiceWithkafka {

  /** aggregate state. it is a generated scalapb message extending the LagompbState trait
   *
   * @return aggregate state
   */
  override def aggregateStateCompanion: GeneratedMessageCompanion[_ <: GeneratedMessage] = TestState

  override def testHello: ServiceCall[TestCmd, TestState] = { req =>
    {
      val companyId: String = UUID.randomUUID().toString
      val cmd = req.update(_.companyUuid := companyId)
      sendCommand[TestCmd, TestState](cmd)
        .map((rst: LagompbState[TestState]) => rst.state)
    }
  }
}

class TestApplicationWithKafka(context: LagomApplicationContext)
    extends LagompbApplication(context)
    with LocalServiceLocator {

  def eventHandler: LagompbEventHandler[TestState] = wire[TestEventHandler]

  def commandHandler: LagompbCommandHandler[TestState] =
    wire[TestCommandHandler]

  override def aggregateRoot: LagompbAggregate[_] = aggregate

  def aggregate: LagompbAggregate[TestState] = wire[TestAggregate]

  /** server helps define the lagom server. Please refer to the lagom doc
   *
   * @example
   * override val server: LagomServer = serverFor[TestService](wire[TestServiceImpl])
   */
  override def server: LagomServer =
    serverFor[TestServiceWithkafka](wire[TestServiceImplWithKafka])
}
