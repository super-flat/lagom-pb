package io.superflat.lagompb.data

import akka.actor.ActorSystem
import io.superflat.lagompb.{AggregateRoot, CommandHandler, EventHandler}
import io.superflat.lagompb.encryption.EncryptionAdapter
import io.superflat.lagompb.protobuf.v1.tests.TestState
import scalapb.GeneratedMessageCompanion

final class TestAggregateRoot(
  actorSystem: ActorSystem,
  commandHandler: CommandHandler[TestState],
  eventHandler: EventHandler[TestState],
  encryptionAdapter: EncryptionAdapter
) extends AggregateRoot[TestState](actorSystem, commandHandler, eventHandler, encryptionAdapter) {

  override def aggregateName: String = "TestAggregate"

  override def stateCompanion: GeneratedMessageCompanion[TestState] = TestState
}
