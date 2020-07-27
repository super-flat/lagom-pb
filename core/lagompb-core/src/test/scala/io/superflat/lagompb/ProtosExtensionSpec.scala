package io.superflat.lagompb

import java.util.UUID

import io.superflat.lagompb.protobuf.extensions.ExtensionsProto
import io.superflat.lagompb.protobuf.tests.{TestCmd, TestEvent}
import io.superflat.lagompb.testkit.BaseSpec
import scalapb.descriptors.FieldDescriptor

class ProtosExtensionSpec extends BaseSpec {
  val companyId: String = UUID.randomUUID().toString

  "Check the existence of kafka option" should {
    val event: TestEvent = TestEvent(companyId, "new state")
    "be successful" in {
      val filter: Seq[FieldDescriptor] =
        event.companion.scalaDescriptor.fields.filter { field =>
          field.getOptions
            .extension(ExtensionsProto.kafka)
            .fold[Boolean](false)(_.partitionKey)
        }

      filter.size should ===(1)
      event.getField(filter.head).as[String] should ===(companyId)
    }
  }

  "Check the existence of entity id" should {
    val cmd = TestCmd().withName("new state").withCompanyUuid(companyId)
    "be successful" in {
      val filter: Seq[FieldDescriptor] =
        cmd.companion.scalaDescriptor.fields.filter { field =>
          field.getOptions
            .extension(ExtensionsProto.command)
            .fold[Boolean](false)(_.entityId)
        }

      filter.size should ===(1)
      cmd.getField(filter.head).as[String] should ===(companyId)
    }
  }
}
