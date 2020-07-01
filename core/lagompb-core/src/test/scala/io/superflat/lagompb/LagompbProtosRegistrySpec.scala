package io.superflat.lagompb

import com.google.protobuf.any.Any
import io.superflat.lagompb.protobuf.core.CoreProto
import io.superflat.lagompb.protobuf.options.OptionsProto
import io.superflat.lagompb.protobuf.tests.TestCmd
import io.superflat.lagompb.testkit.LagompbSpec
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

class LagompbProtosRegistrySpec extends LagompbSpec {

  "Loading GeneratedFileObject" should {

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
