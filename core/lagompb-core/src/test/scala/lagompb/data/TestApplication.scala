package lagompb.data

import com.lightbend.lagom.scaladsl.server.{LagomApplicationContext, LagomServer, LocalServiceLocator}
import com.softwaremill.macwire.wire
import lagompb.{LagompbAggregate, LagompbApplication, LagompbCommandHandler, LagompbEventHandler}
import lagompb.tests.TestState

class TestApplication(context: LagomApplicationContext) extends LagompbApplication(context) with LocalServiceLocator {

  def eventHandler: LagompbEventHandler[TestState] = wire[TestEventHandler]

  def commandHandler: LagompbCommandHandler[TestState] =
    wire[TestCommandHandler]

  def aggregate: LagompbAggregate[TestState] = wire[TestAggregate]

  override def aggregateRoot: LagompbAggregate[_] = aggregate

  /** server helps define the lagom server. Please refer to the lagom doc
   *
   * @example
   * override val server: LagomServer = serverFor[TestService](wire[TestServiceImpl])
   */
  override def server: LagomServer =
    serverFor[TestService](wire[TestServiceImpl])
}
