package io.superflat.lagompb

import java.util.UUID

import akka.actor.typed.scaladsl.adapter._
import akka.actor.ActorSystem
import io.superflat.lagompb.data.TestEventHandler
import io.superflat.lagompb.testkit.BaseActorTestKit
import io.superflat.lagompb.protobuf.v1.core.MetaData
import io.superflat.lagompb.protobuf.v1.tests.{TestEvent, TestState, WrongEvent}
import com.google.protobuf.any.Any

class EventHandlerSpec extends BaseActorTestKit(s"""
      akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      akka.persistence.snapshot-store.local.dir = "tmp/snapshot"
    """) {

  val actorSystem: ActorSystem = testKit.system.toClassic

  "EventHandler implementation" must {
    val companyId: String = UUID.randomUUID().toString
    val eventHandler: TestEventHandler = new TestEventHandler(actorSystem)

    "handle event and return the new state" in {
      val prevState: TestState = TestState(companyId, "state")
      val event: TestEvent = TestEvent(companyId, "new state")

      val result: Any =
        eventHandler.handle(Any.pack(event), Any.pack(prevState), MetaData.defaultInstance)
      result.unpack[TestState] should be(TestState(companyId, "new state"))
    }

    "handle wrong event" in {
      val prevState: TestState = TestState(companyId, "state")
      assertThrows[NotImplementedError](
        eventHandler.handle(Any.pack(WrongEvent()), Any.pack(prevState), MetaData.defaultInstance)
      )
    }
  }
}
