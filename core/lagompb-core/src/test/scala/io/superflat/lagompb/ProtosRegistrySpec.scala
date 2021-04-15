/*
 * Copyright (C) 2020 Superflat. <https://github.com/super-flat>
 */

package io.superflat.lagompb

import com.google.protobuf.any.Any
import io.superflat.lagompb.protobuf.v1.core.CoreProto
import io.superflat.lagompb.protobuf.v1.tests.TestCommand
import io.superflat.lagompb.testkit.BaseSpec
import scalapb.{ GeneratedMessage, GeneratedMessageCompanion }

class ProtosRegistrySpec extends BaseSpec {

  "The Protos registry" should {

    "load protos definitions as expected" in {
      val fos = ProtosRegistry.registry
      fos.size should be >= 1
      fos should contain(CoreProto)
    }

    "load message companions as expected" in {
      val size: Int = ProtosRegistry.companions.size
      size should be >= 1
    }

    "help build the message companions map as expected" in {
      val map: Map[String, GeneratedMessageCompanion[_ <: GeneratedMessage]] =
        ProtosRegistry.companionsMap
      map.keySet should contain("lagompb.v1.TestCommand")
    }

    "help get scalapb GeneratedMessageCompanion object" in {
      val any = Any.pack(TestCommand.defaultInstance)
      ProtosRegistry.getCompanion(any) should be(Symbol("defined"))
    }

    "help convert a proto message to json" in {
      val testCommand = TestCommand.defaultInstance.withCompanyUuid("123").withName("test")
      val json = ProtosRegistry.toJson(testCommand)
      json shouldBe """{"companyUuid":"123","name":"test"}""".stripMargin
    }

    "help convert a json string to a proto message" in {
      val jsonString = """{"companyUuid":"123","name":"test"}""".stripMargin
      val testCommand = ProtosRegistry.fromJson[TestCommand](jsonString)
      testCommand.companyUuid shouldBe "123"
      testCommand.name shouldBe "test"
    }
  }
}
