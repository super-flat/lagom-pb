package io.superflat.lagompb

import com.google.protobuf.any.Any
import io.superflat.lagompb.testkit.BaseSpec
import io.superflat.lagompb.protobuf.v1.core.CoreProto

import io.superflat.lagompb.protobuf.v1.tests.TestCmd
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

class ProtosRegistrySpec extends BaseSpec {

  "Loading GeneratedFileObject" should {

    val protosRegistry = ProtosRegistry.fromReflection()
    "succeed" in {
      val fos = protosRegistry.registry
      fos.size should be >= 1
      fos should contain(CoreProto)
    }

    "should be loaded" in {
      val size: Int = protosRegistry.companions.size
      size should be >= 1
    }

    "Contains the companions" in {
      val map: Map[String, GeneratedMessageCompanion[_ <: GeneratedMessage]] =
        protosRegistry.companionsMap
      map.keySet should contain("lagompb.v1.TestCmd")
    }

    "Gets scalapb GeneratedMessageCompanion object" in {
      val any = Any.pack(TestCmd.defaultInstance)
      protosRegistry.getCompanion(any) should be(Symbol("defined"))
    }
  }
}
