package io.superflat.lagompb

import akka.util.ByteString
import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer.{NegotiatedDeserializer, NegotiatedSerializer}
import com.lightbend.lagom.scaladsl.api.deser.{MessageSerializer, StrictMessageSerializer}
import com.lightbend.lagom.scaladsl.api.transport.MessageProtocol
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

/**
 * ApiSerializer helps serialize json REST payload into protobuf messages and vice versa.
 */
case class ApiSerializer[A <: GeneratedMessage: GeneratedMessageCompanion]()
    extends StrictMessageSerializer[A]
    with GenericSerializers[A] {

  override def serializerForRequest: MessageSerializer.NegotiatedSerializer[A, ByteString] = serializerJson

  override def deserializer(
      protocol: MessageProtocol
  ): MessageSerializer.NegotiatedDeserializer[A, ByteString] =
    deserializer

  override def serializerForResponse(
      acceptedMessageProtocols: Seq[MessageProtocol]
  ): MessageSerializer.NegotiatedSerializer[A, ByteString] =
    negotiateResponse(acceptedMessageProtocols)
}

sealed trait GenericSerializers[T <: GeneratedMessage] {

  def deserializer(implicit
      T: GeneratedMessageCompanion[T]
  ): NegotiatedDeserializer[T, ByteString] = { (wire: ByteString) =>

    ProtosRegistry.parser.fromJsonString(wire.utf8String)
  }

  def negotiateResponse(
      acceptedMessageProtocols: Seq[MessageProtocol]
  ): NegotiatedSerializer[T, ByteString] =
    acceptedMessageProtocols match {
      case Nil => serializerJson
      case protocols =>
        protocols
          .collectFirst {
            case MessageProtocol(Some("application/x-protobuf"), _, _) =>
              serializerProtobuf
            case _ => serializerJson
          }
          .getOrElse(serializerJson)
    }

  def serializerJson: NegotiatedSerializer[T, ByteString] =
    new NegotiatedSerializer[T, ByteString] {

      override def protocol: MessageProtocol =
        MessageProtocol(Some("application/json"))

      override def serialize(message: T): ByteString =
        ByteString(ProtosRegistry.printer.print(message))
    }

  def serializerProtobuf: NegotiatedSerializer[T, ByteString] =
    new NegotiatedSerializer[T, ByteString] {

      override def protocol: MessageProtocol =
        MessageProtocol(Some("application/x-protobuf"))

      override def serialize(message: T): ByteString =
        ByteString(message.toByteArray)
    }
}
