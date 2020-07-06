package io.superflat.lagompb.data

import akka.actor.ActorSystem
import io.superflat.lagompb.{AggregateRoot, CommandHandler, EventHandler}
import io.superflat.lagompb.protobuf.tests.TestState
import scalapb.GeneratedMessageCompanion

final class TestAggregateRoot(
    actorSystem: ActorSystem,
    commandHandler: CommandHandler[TestState],
    eventHandler: EventHandler[TestState]
) extends AggregateRoot[TestState](actorSystem, commandHandler, eventHandler) {

  override def aggregateName: String = "TestAggregate"

  override def stateCompanion: GeneratedMessageCompanion[TestState] = TestState
}
