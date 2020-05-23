package lagompb.testkit

import org.scalamock.scalatest.MockFactory
import org.scalatest._
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpecLike

trait LagompbSpec
    extends AnyWordSpecLike
    with Matchers
    with TestSuite
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with OptionValues
    with TryValues
    with ScalaFutures
    with Eventually
    with MockFactory {
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(30, Seconds), interval = Span(100, org.scalatest.time.Millis))
}
