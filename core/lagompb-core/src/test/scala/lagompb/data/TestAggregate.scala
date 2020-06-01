package lagompb.data

import akka.actor.ActorSystem
import com.typesafe.config.Config
import lagompb.tests.TestState
import lagompb.{LagompbAggregate, LagompbCommandHandler, LagompbEventHandler}
import scalapb.GeneratedMessageCompanion

final class TestAggregate(
    actorSystem: ActorSystem,
    config: Config,
    commandHandler: LagompbCommandHandler[TestState],
    eventHandler: LagompbEventHandler[TestState]
) extends LagompbAggregate[TestState](actorSystem, config, commandHandler, eventHandler) {

  override def aggregateName: String = "TestAggregate"

  override def stateCompanion: GeneratedMessageCompanion[TestState] = TestState
}
