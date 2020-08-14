package io.superflat.lagompb

import akka.actor.testkit.typed.scaladsl.TestProbe
import io.superflat.lagompb.testkit.BaseActorTestKit
import io.superflat.lagompb.v1.protobuf.core.CommandReply
import io.superflat.lagompb.v1.protobuf.tests.TestCmd

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
    val command = TestCmd(companyUUID, "John Ross")
    val data =
      Map("audit|employeeUuid" -> "1223", "audit|createdAt" -> "2020-04-17")
    serializationTestKit.verifySerialization(Command(command, probe.ref, data))
  }
}
