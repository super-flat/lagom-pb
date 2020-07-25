package io.superflat.lagompb

import com.google.protobuf.any.Any
import io.superflat.lagompb.protobuf.core.CoreProto
import io.superflat.lagompb.protobuf.options.OptionsProto
import io.superflat.lagompb.protobuf.tests.TestCmd
import io.superflat.lagompb.testkit.LagompbSpec
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

class ProtosRegistrySpec extends LagompbSpec {

  "Loading GeneratedFileObject" should {

    "succeed" in {
      val fos = ProtosRegistry.registry
      fos.size should be >= 1
      fos should contain(OptionsProto)
      fos should contain(CoreProto)
    }

    "should be loaded" in {
      val size: Int = ProtosRegistry.companions.size
      size should be >= 1
    }

    "Contains the companions" in {
      val map: Map[String, GeneratedMessageCompanion[_ <: GeneratedMessage]] =
        ProtosRegistry.companionsMap
      map.keySet should contain("lagompb.TestCmd")
    }

    "Gets scalapb GeneratedMessageCompanion object" in {
      val any = Any.pack(TestCmd.defaultInstance)
      ProtosRegistry.companion(any) should be(Symbol("defined"))
    }
  }
}
