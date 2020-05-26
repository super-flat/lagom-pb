package lagompb.data

import java.util.UUID

import akka.actor.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.lightbend.lagom.scaladsl.api.Descriptor
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.Service.restCall
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.lightbend.lagom.scaladsl.server.LagomApplicationContext
import com.lightbend.lagom.scaladsl.server.LagomServer
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.softwaremill.macwire.wire
import lagompb.LagompbAggregate
import lagompb.LagompbApplication
import lagompb.LagompbCommandHandler
import lagompb.LagompbEventHandler
import lagompb.LagompbService
import lagompb.LagompbServiceImpl
import lagompb.LagompbState
import lagompb.protobuf.tests.TestCmd
import lagompb.protobuf.tests.TestState
import scalapb.GeneratedMessage
import scalapb.GeneratedMessageCompanion

import scala.concurrent.ExecutionContext

trait TestService extends LagompbService {
  def testHello: ServiceCall[TestCmd, TestState]

  /** routes define the various routes handled by the service.
   *
   *
   */
  override def routes: Seq[Descriptor.Call[_, _]] = Seq(
    restCall(Method.POST, "/api/tests", testHello _),
  )
}

class TestServiceImpl(
    sys: ActorSystem,
    clusterSharding: ClusterSharding,
    persistentEntityRegistry: PersistentEntityRegistry,
    aggregate: LagompbAggregate[TestState]
)(
    implicit ec: ExecutionContext
) extends LagompbServiceImpl(clusterSharding, persistentEntityRegistry, aggregate)
    with TestService {

  /** aggregate state. it is a generated scalapb message extending the LagompbState trait
   *
   * @return aggregate state
   */
  override def aggregateStateCompanion: GeneratedMessageCompanion[_ <: GeneratedMessage] = TestState

  override def testHello: ServiceCall[TestCmd, TestState] = { req =>
    {
      val companyId: String = UUID.randomUUID().toString
      val cmd = req.update(
        _.companyUuid := companyId
      )
      sendCommand[TestCmd, TestState](cmd).map(
        (rst: LagompbState[TestState]) => rst.state
      )
    }
  }
}

class TestApplication(context: LagomApplicationContext) extends LagompbApplication(context) with LocalServiceLocator {

  def eventHandler: LagompbEventHandler[TestState] = wire[TestEventHandler]
  def commandHandler: LagompbCommandHandler[TestState] = wire[TestCommandHandler]
  def aggregate: LagompbAggregate[TestState] = wire[TestAggregate]

  override def aggregateRoot: LagompbAggregate[_] = aggregate

  /** server helps define the lagom server. Please refer to the lagom doc
   *
   * @example
   * override val server: LagomServer = serverFor[TestService](wire[TestServiceImpl])
   */
  override def server: LagomServer = serverFor[TestService](wire[TestServiceImpl])
}
