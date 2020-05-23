package lagompb.data

import akka.actor.ActorSystem
import lagompb.LagompbAggregate
import lagompb.LagompbCommandHandler
import lagompb.LagompbEventHandler
import lagompb.protobuf.tests.TestState
import scalapb.GeneratedMessageCompanion
import com.typesafe.config.Config

final class TestAggregate(
    actorSystem: ActorSystem,
    config: Config,
    commandHandler: LagompbCommandHandler[TestState],
    eventHandler: LagompbEventHandler[TestState]
) extends LagompbAggregate[TestState](actorSystem, config, commandHandler, eventHandler) {

  override def aggregateName: String = "TestAggregate"

  override def stateCompanion: GeneratedMessageCompanion[TestState] = TestState
}
