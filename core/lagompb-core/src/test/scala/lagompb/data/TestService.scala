package lagompb.data

import java.util.UUID

import akka.actor.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.lightbend.lagom.scaladsl.api.{Descriptor, ServiceCall}
import com.lightbend.lagom.scaladsl.api.Service.restCall
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import lagompb.{LagompbAggregate, LagompbSerializer, LagompbService, LagompbServiceImpl, LagompbState}
import lagompb.tests.{TestCmd, TestState}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

import scala.concurrent.ExecutionContext

trait TestService extends LagompbService {

  def testHello: ServiceCall[TestCmd, TestState]

  /**
   * routes define the various routes handled by the service.
   */
  override def routes: Seq[Descriptor.Call[_, _]] =
    Seq(restCall(Method.POST, "/api/tests", testHello _))
}

class TestServiceImpl(
    sys: ActorSystem,
    clusterSharding: ClusterSharding,
    persistentEntityRegistry: PersistentEntityRegistry,
    aggregate: LagompbAggregate[TestState]
)(implicit ec: ExecutionContext)
    extends LagompbServiceImpl(clusterSharding, persistentEntityRegistry, aggregate)
    with TestService {

  /**
   * aggregate state. it is a generated scalapb message extending the LagompbState trait
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
