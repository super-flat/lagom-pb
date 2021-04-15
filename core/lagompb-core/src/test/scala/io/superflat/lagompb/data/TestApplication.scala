/*
 * Copyright (C) 2020 Superflat. <https://github.com/super-flat>
 */

package io.superflat.lagompb.data

import com.lightbend.lagom.scaladsl.server.{ LagomApplicationContext, LagomServer, LocalServiceLocator }
import com.softwaremill.macwire.wire
import io.superflat.lagompb.protobuf.v1.tests.TestState
import io.superflat.lagompb.{ AggregateRoot, BaseApplication, TypedCommandHandler, TypedEventHandler }

class TestApplication(context: LagomApplicationContext) extends BaseApplication(context) with LocalServiceLocator {

  def eventHandler: TypedEventHandler[TestState] = wire[TestEventHandler]

  def commandHandler: TypedCommandHandler[TestState] = wire[TestCommandHandler]

  lazy val aggregateRoot: AggregateRoot =
    new TestAggregateRoot(actorSystem, commandHandler, eventHandler, TestState(), encryptionAdapter)

  startAggregateRootCluster()

  /**
   * server helps define the lagom server. Please refer to the lagom doc
   *
   * @example
   * override val server: LagomServer = serverFor[TestService](wire[TestServiceImpl])
   */
  override lazy val server: LagomServer =
    serverFor[TestService](wire[TestServiceImpl])
}
