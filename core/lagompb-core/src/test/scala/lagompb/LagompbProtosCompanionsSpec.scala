package lagompb

import com.google.protobuf.any.Any
import lagompb.tests.TestCmd
import lagompb.testkit.LagompbSpec
import lagompb.util.LagompbProtosCompanions
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

class LagompbProtosCompanionsSpec extends LagompbSpec {
  "Protos Companions" should {
    "should be loaded" in {
      val size: Int = LagompbProtosCompanions.companions.size
      size should be >= 1
    }

    "Contains the companions" in {
      val map: Map[String, GeneratedMessageCompanion[_ <: GeneratedMessage]] =
        LagompbProtosCompanions.companionsMap
      map.keySet should contain("lagompb.TestCmd")
    }

    "Gets scalapb GeneratedMessageCompanion object" in {
      val any = Any.pack(TestCmd.defaultInstance)
      LagompbProtosCompanions.getCompanion(any) should be(Symbol("defined"))
    }
  }
}
