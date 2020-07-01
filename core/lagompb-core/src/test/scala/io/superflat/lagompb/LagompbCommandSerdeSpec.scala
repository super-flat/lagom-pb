package io.superflat.lagompb

import akka.actor.testkit.typed.scaladsl.TestProbe
import io.superflat.lagompb.protobuf.core.CommandReply
import io.superflat.lagompb.protobuf.tests.TestCmd
import io.superflat.lagompb.testkit.LagompbActorTestKit

class LagompbCommandSerdeSpec extends LagompbActorTestKit(s"""
    akka {
      actor {
        serialize-messages = on
        serializers {
          proto = "akka.remote.serialization.ProtobufSerializer"
          cmdSerializer = "io.superflat.lagompb.LagompbCommandSerde"
        }
        serialization-bindings {
          "scalapb.GeneratedMessage" = proto
          "io.superflat.lagompb.LagompbCommand" = cmdSerializer
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
    serializationTestKit.verifySerialization(LagompbCommand(command, probe.ref, data))
  }
}
