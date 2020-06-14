package lagompb

import com.google.protobuf.any.Any
import lagompb.core.CoreProto
import lagompb.options.OptionsProto
import lagompb.testkit.LagompbSpec
import lagompb.tests.TestCmd
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

class LagompbProtosRegistrySpec extends LagompbSpec {

  "Loading GeneratedFileObject" should {
    LagompbProtosRegistry.init()
    "succeed" in {
      val fos = LagompbProtosRegistry.registry
      fos.size should be >= 1
      fos should contain(OptionsProto)
      fos should contain(CoreProto)
    }

    "should be loaded" in {
      val size: Int = LagompbProtosRegistry.companions.size
      size should be >= 1
    }

    "Contains the companions" in {
      val map: Map[String, GeneratedMessageCompanion[_ <: GeneratedMessage]] =
        LagompbProtosRegistry.companionsMap
      map.keySet should contain("lagompb.TestCmd")
    }

    "Gets scalapb GeneratedMessageCompanion object" in {
      val any = Any.pack(TestCmd.defaultInstance)
      LagompbProtosRegistry.getCompanion(any) should be(Symbol("defined"))
    }
  }
}
