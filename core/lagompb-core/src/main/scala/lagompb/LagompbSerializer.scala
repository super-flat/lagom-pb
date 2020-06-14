package lagompb

import akka.util.ByteString
import com.lightbend.lagom.scaladsl.api.deser.{MessageSerializer, StrictMessageSerializer}
import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer.NegotiatedSerializer
import com.lightbend.lagom.scaladsl.api.transport.MessageProtocol
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

class LagompbSerializer[T <: GeneratedMessage: GeneratedMessageCompanion] extends StrictMessageSerializer[T] {

  private final val serializerJson = {
    new NegotiatedSerializer[T, ByteString]() {
      override def protocol: MessageProtocol =
        MessageProtocol(Some("application/json"))

      def serialize(message: T): ByteString = {
        ByteString(LagompbProtosRegistry.printer.print(message))
      }
    }
  }

  override def serializerForRequest: MessageSerializer.NegotiatedSerializer[T, ByteString] = serializerJson

  override def deserializer(protocol: MessageProtocol): MessageSerializer.NegotiatedDeserializer[T, ByteString] = {
    (wire: ByteString) =>
      {
        LagompbProtosRegistry.parser.fromJsonString(wire.utf8String)
      }
  }

  override def serializerForResponse(
      acceptedMessageProtocols: Seq[MessageProtocol]
  ): MessageSerializer.NegotiatedSerializer[T, ByteString] = serializerJson
}
