package io.superflat.lagompb.data

import akka.actor.ActorSystem
import io.superflat.lagompb.{LagompbAggregate, LagompbCommandHandler, LagompbEventHandler}
import io.superflat.lagompb.protobuf.tests.TestState
import scalapb.GeneratedMessageCompanion

final class TestAggregate(
    actorSystem: ActorSystem,
    commandHandler: LagompbCommandHandler[TestState],
    eventHandler: LagompbEventHandler[TestState]
) extends LagompbAggregate[TestState](actorSystem, commandHandler, eventHandler) {

  override def aggregateName: String = "TestAggregate"

  override def stateCompanion: GeneratedMessageCompanion[TestState] = TestState
}
