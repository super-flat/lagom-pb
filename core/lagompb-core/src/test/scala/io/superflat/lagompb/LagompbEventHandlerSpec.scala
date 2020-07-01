package io.superflat.lagompb

import java.util.UUID

import io.superflat.lagompb.data.TestEventHandler
import io.superflat.lagompb.protobuf.core.MetaData
import io.superflat.lagompb.protobuf.tests.{TestEvent, TestState, WrongEvent}
import io.superflat.lagompb.testkit.LagompbSpec

class LagompbEventHandlerSpec extends LagompbSpec {
  "BaseEventHandler implementation" must {
    val companyId: String = UUID.randomUUID().toString
    val eventHandler = new TestEventHandler(null)

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
