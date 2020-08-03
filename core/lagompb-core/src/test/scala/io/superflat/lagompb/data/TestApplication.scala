package io.superflat.lagompb.data

import com.lightbend.lagom.scaladsl.server.{LagomApplicationContext, LagomServer, LocalServiceLocator}
import com.softwaremill.macwire.wire
import io.superflat.lagompb.{AggregateRoot, BaseApplication, CommandHandler, EventHandler}
import io.superflat.lagompb.protobuf.tests.TestState
import io.superflat.lagompb.encryption.EncryptionAdapter

class TestApplication(context: LagomApplicationContext) extends BaseApplication(context) with LocalServiceLocator {

  def eventHandler: EventHandler[TestState] = wire[TestEventHandler]

  def commandHandler: CommandHandler[TestState] = wire[TestCommandHandler]

  def encryptionAdapter: EncryptionAdapter = new EncryptionAdapter(encryptor = None)

  def aggregate: AggregateRoot[TestState] = wire[TestAggregateRoot]

  override def aggregateRoot: AggregateRoot[_] = aggregate

  /**
   * server helps define the lagom server. Please refer to the lagom doc
   *
   * @example
   * override val server: LagomServer = serverFor[TestService](wire[TestServiceImpl])
   */
  override def server: LagomServer =
    serverFor[TestService](wire[TestServiceImpl])
}
