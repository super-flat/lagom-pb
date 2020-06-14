package lagompb.data

import akka.actor.ActorSystem
import lagompb.{LagompbAggregate, LagompbCommandHandler, LagompbEventHandler}
import lagompb.tests.TestState
import scalapb.GeneratedMessageCompanion

final class TestAggregate(
    actorSystem: ActorSystem,
    commandHandler: LagompbCommandHandler[TestState],
    eventHandler: LagompbEventHandler[TestState]
) extends LagompbAggregate[TestState](actorSystem, commandHandler, eventHandler) {

  override def aggregateName: String = "TestAggregate"

  override def stateCompanion: GeneratedMessageCompanion[TestState] = TestState
}
