/*
 * Copyright (C) 2020 Superflat. <https://github.com/super-flat>
 */

package io.superflat.lagompb

import akka.actor.testkit.typed.scaladsl.TestProbe
import com.google.protobuf.any.Any
import io.superflat.lagompb.protobuf.v1.core.CommandReply
import io.superflat.lagompb.protobuf.v1.tests.{ AuditingData, TestCommand }
import io.superflat.lagompb.testkit.BaseActorTestKit

class CommandSerializerSpec extends BaseActorTestKit(s"""
    akka {
      actor {
        serialize-messages = on
        serializers {
          proto = "akka.remote.serialization.ProtobufSerializer"
          cmdSerializer = "io.superflat.lagompb.CommandSerializer"
        }
        serialization-bindings {
          "scalapb.GeneratedMessage" = proto
          "io.superflat.lagompb.Command" = cmdSerializer
        }
      }
    }
    """) {
  private val companyUUID = "93cfb5fc-c01b-4cda-bb45-31875bafda23"

  "Verification of command serializer" in {
    val probe: TestProbe[CommandReply] = createTestProbe[CommandReply]()
    val command = TestCommand(companyUUID, "John Ross")
    val auditing = AuditingData.defaultInstance.withData(Map("employeeUuid" -> "123"))
    val data = Map("audit" -> Any.pack(auditing))
    serializationTestKit.verifySerialization(Command(Any.pack(command), probe.ref, data))
  }
}
