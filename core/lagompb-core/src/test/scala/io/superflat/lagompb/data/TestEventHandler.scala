package io.superflat.lagompb.data

import akka.actor.ActorSystem
import io.superflat.lagompb.EventHandler
import io.superflat.lagompb.protobuf.core.MetaData
import io.superflat.lagompb.protobuf.tests.{TestEvent, TestState}

class TestEventHandler(actorSystem: ActorSystem)
    extends EventHandler[TestState](actorSystem) {

  override def handle(
      event: scalapb.GeneratedMessage,
      currentState: TestState,
      eventMeta: MetaData
  ): TestState =
    event match {
      case TestEvent(companyUuid, name) =>
        handleTestEvent(companyUuid, name, currentState)
      case _ => throw new NotImplementedError()
    }

  private[this] def handleTestEvent(
      companyUuid: String,
      name: String,
      state: TestState
  ): TestState =
    state.copy(companyUuid = companyUuid, name = name)
}
