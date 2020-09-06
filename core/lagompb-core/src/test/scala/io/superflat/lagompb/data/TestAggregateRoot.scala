package io.superflat.lagompb.data

import akka.actor.ActorSystem
import io.superflat.lagompb.encryption.EncryptionAdapter
import io.superflat.lagompb.protobuf.v1.tests.TestState
import io.superflat.lagompb.{AggregateRoot, TypedCommandHandler, TypedEventHandler}

final class TestAggregateRoot(
  actorSystem: ActorSystem,
  commandHandler: TypedCommandHandler[TestState],
  eventHandler: TypedEventHandler[TestState],
  initialState: TestState,
  encryptionAdapter: EncryptionAdapter
) extends AggregateRoot(actorSystem, commandHandler, eventHandler, initialState, encryptionAdapter) {

  override def aggregateName: String = "TestAggregate"
}
