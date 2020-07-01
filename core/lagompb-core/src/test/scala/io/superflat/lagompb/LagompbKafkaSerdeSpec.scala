package io.superflat.lagompb

import java.util.UUID

import akka.util.ByteString
import com.google.protobuf.any.Any
import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer
import com.lightbend.lagom.scaladsl.api.transport.MessageProtocol
import io.superflat.lagompb.protobuf.core.{KafkaEvent, MetaData, StateWrapper}
import io.superflat.lagompb.protobuf.tests.{TestEvent, TestState}
import io.superflat.lagompb.testkit.LagompbSpec

class LagompbKafkaSerdeSpec extends LagompbSpec {
  val serviceEventSerializer: LagompbKafkaSerde = new LagompbKafkaSerde

  val reqSerializer: MessageSerializer.NegotiatedSerializer[KafkaEvent, ByteString] =
    serviceEventSerializer.serializerForRequest

  val respSerializer: MessageSerializer.NegotiatedSerializer[KafkaEvent, ByteString] =
    serviceEventSerializer.serializerForResponse(Seq(MessageProtocol.empty))

  val deserializer: MessageSerializer.NegotiatedDeserializer[KafkaEvent, ByteString] =
    serviceEventSerializer.deserializer(MessageProtocol.empty)

  val eventId: String = UUID.randomUUID().toString
  val companyId: String = UUID.randomUUID().toString

  val anyEvent: Any = Any.pack(
    TestEvent()
      .withEventUuid(companyId)
      .withName("test")
  )

  val anyState: Any =
    Any.pack(TestState().withName("test").withCompanyUuid(companyId))

  val stateWrapper: StateWrapper = StateWrapper()
    .withMeta(MetaData().withRevisionNumber(1))
    .withState(anyState)

  val event: KafkaEvent = KafkaEvent()
    .withServiceName("test")
    .withPartitionKey(eventId)
    .withEvent(anyEvent)
    .withState(stateWrapper)

  val bstr: String = event.toByteString.toStringUtf8

  "KafkaSerde" must {
    "serialize a Service Event request" in {
      val serialized: String = reqSerializer.serialize(event).utf8String

      serialized should ===(bstr)
    }

    "deserialize a wired Service Event" in {
      val serialized: ByteString = reqSerializer.serialize(event)
      val deserialized: KafkaEvent = deserializer.deserialize(serialized)

      deserialized should ===(event)
    }

    "serialize a Service Event response" in {
      val serialized: ByteString = respSerializer.serialize(event)
      serialized.utf8String should ===(bstr)
    }
  }
}
