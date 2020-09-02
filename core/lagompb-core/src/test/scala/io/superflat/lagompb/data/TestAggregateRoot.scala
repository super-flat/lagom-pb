package io.superflat.lagompb.data

import akka.actor.ActorSystem
import io.superflat.lagompb.{AggregateRoot, TypedCommandHandler, TypedEventHandler}
import io.superflat.lagompb.encryption.EncryptionAdapter
import io.superflat.lagompb.protobuf.v1.tests.TestState
import scalapb.GeneratedMessageCompanion

final class TestAggregateRoot(
  actorSystem: ActorSystem,
  commandHandler: TypedCommandHandler[TestState],
  eventHandler: TypedEventHandler[TestState],
  encryptionAdapter: EncryptionAdapter
) extends AggregateRoot[TestState](actorSystem, commandHandler, eventHandler, encryptionAdapter) {

  override def aggregateName: String = "TestAggregate"

  override def stateCompanion: GeneratedMessageCompanion[TestState] = TestState
}
