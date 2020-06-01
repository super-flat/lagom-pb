package lagompb.data

import akka.actor.ActorSystem
import lagompb.LagompbEventHandler
import lagompb.core.MetaData
import lagompb.tests.{TestEvent, TestState}

class TestEventHandler(actorSystem: ActorSystem) extends LagompbEventHandler[TestState](actorSystem) {

  override def handle(event: scalapb.GeneratedMessage, currentState: TestState, eventMeta: MetaData): TestState = {
    event match {
      case TestEvent(companyUuid, name) =>
        handleTestEvent(companyUuid, name, currentState)
      case _ => throw new NotImplementedError()
    }
  }

  private def handleTestEvent(companyUuid: String, name: String, state: TestState): TestState = {
    state.copy(companyUuid = companyUuid, name = name)
  }
}
