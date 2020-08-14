package io.superflat.lagompb

import java.util.UUID

import akka.actor.typed.scaladsl.adapter._
import akka.actor.ActorSystem
import io.superflat.lagompb.data.TestEventHandler
import io.superflat.lagompb.testkit.BaseActorTestKit
import io.superflat.lagompb.protobuf.v1.core.MetaData
import io.superflat.lagompb.protobuf.v1.tests.{TestEvent, TestState, WrongEvent}

class EventHandlerSpec extends BaseActorTestKit(s"""
      akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      akka.persistence.snapshot-store.local.dir = "tmp/snapshot"
    """) {

  val actorSystem: ActorSystem = testKit.system.toClassic

  "EventHandler implementation" must {
    val companyId: String = UUID.randomUUID().toString
    val eventHandler = new TestEventHandler(actorSystem)

    "handle event and return the new state" in {
      val prevState = TestState(companyId, "state")
      val event = TestEvent(companyId, "new state")

      val result: TestState =
        eventHandler.handle(event, prevState, MetaData.defaultInstance)
      result should be(TestState(companyId, "new state"))
    }

    "handle wrong event" in {
      val prevState = TestState(companyId, "state")
      assertThrows[NotImplementedError](eventHandler.handle(WrongEvent(), prevState, MetaData.defaultInstance))
    }
  }
}
