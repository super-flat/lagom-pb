package io.superflat.lagompb.data

import akka.actor.ActorSystem
import io.superflat.lagompb.EventHandler
import io.superflat.lagompb.protobuf.v1.core.MetaData
import io.superflat.lagompb.protobuf.v1.tests.{TestEvent, TestEventFailure, TestState}

class TestEventHandler(actorSystem: ActorSystem) extends EventHandler[TestState](actorSystem) {

  override def handle(event: scalapb.GeneratedMessage, currentState: TestState, eventMeta: MetaData): TestState =
    event match {
      case TestEvent(companyUuid, name, _) =>
        handleTestEvent(companyUuid, name, currentState)
      case TestEventFailure(_) => throw new NotImplementedError()
      case _                   => throw new NotImplementedError()
    }

  private[this] def handleTestEvent(companyUuid: String, name: String, state: TestState): TestState =
    state.copy(companyUuid = companyUuid, name = name)
}
