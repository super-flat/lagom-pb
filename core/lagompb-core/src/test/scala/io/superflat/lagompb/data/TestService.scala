/*
 * Copyright (C) 2020 Superflat. <https://github.com/super-flat>
 */

package io.superflat.lagompb.data

import java.util.UUID

import akka.actor.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.google.protobuf.any.Any
import com.lightbend.lagom.scaladsl.api.Service.restCall
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, ServiceCall }
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import io.superflat.lagompb.protobuf.v1.core.StateWrapper
import io.superflat.lagompb.protobuf.v1.tests.{ TestCommand, TestState }
import io.superflat.lagompb.{ AggregateRoot, BaseService, BaseServiceImpl }

import scala.concurrent.ExecutionContext

trait TestService extends BaseService {

  def testHello: ServiceCall[TestCommand, TestState]

  /**
   * routes define the various routes handled by the service.
   */
  override def routes: Seq[Descriptor.Call[_, _]] =
    Seq(restCall(Method.POST, "/api/tests", testHello))
}

class TestServiceImpl(
    sys: ActorSystem,
    clusterSharding: ClusterSharding,
    persistentEntityRegistry: PersistentEntityRegistry,
    aggregateRoot: AggregateRoot)(implicit ec: ExecutionContext)
    extends BaseServiceImpl(clusterSharding, persistentEntityRegistry, aggregateRoot)
    with TestService {

  override def testHello: ServiceCall[TestCommand, TestState] = { req =>
    val companyId: String = UUID.randomUUID().toString
    val cmd = req.update(_.companyUuid := companyId)
    sendCommand(companyId, cmd, Map.empty[String, Any]).map((rst: StateWrapper) => rst.state.get.unpack(TestState))
  }
}
