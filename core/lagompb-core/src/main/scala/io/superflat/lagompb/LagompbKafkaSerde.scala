package io.superflat.lagompb

import akka.util.ByteString
import com.lightbend.lagom.scaladsl.api.deser.{MessageSerializer, StrictMessageSerializer}
import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer.{NegotiatedDeserializer, NegotiatedSerializer}
import com.lightbend.lagom.scaladsl.api.transport.MessageProtocol
import io.superflat.lagompb.protobuf.core.KafkaEvent

/**
 * Serializer to push messages to kafka from message broker
 */
class LagompbKafkaSerde extends StrictMessageSerializer[KafkaEvent] {

  private final val serializer: NegotiatedSerializer[KafkaEvent, ByteString] = { (serviceEvent: KafkaEvent) =>
    {
      val builder = ByteString.createBuilder
      serviceEvent.writeTo(builder.asOutputStream)
      builder.result
    }
  }

  private final val deserializer: NegotiatedDeserializer[KafkaEvent, ByteString] = { (bytes: ByteString) =>
    KafkaEvent.parseFrom(bytes.iterator.asInputStream)
  }

  override def serializerForRequest: MessageSerializer.NegotiatedSerializer[KafkaEvent, ByteString] =
    serializer

  override def deserializer(
      protocol: MessageProtocol
  ): MessageSerializer.NegotiatedDeserializer[KafkaEvent, ByteString] =
    deserializer

  override def serializerForResponse(
      acceptedMessageProtocols: Seq[MessageProtocol]
  ): MessageSerializer.NegotiatedSerializer[KafkaEvent, ByteString] = serializer
}
