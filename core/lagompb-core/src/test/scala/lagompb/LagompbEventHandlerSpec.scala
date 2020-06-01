package lagompb

import java.util.UUID

import lagompb.data.TestEventHandler
import lagompb.core.MetaData
import lagompb.tests.{TestEvent, TestState, WrongEvent}
import lagompb.testkit.LagompbSpec

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
