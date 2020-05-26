package lagompb

import java.nio.charset.StandardCharsets

import akka.actor.ExtendedActorSystem
import akka.actor.typed.{ActorRef, ActorRefResolver}
import akka.actor.typed.scaladsl.adapter._
import akka.serialization.SerializerWithStringManifest
import com.google.protobuf.any.Any
import lagompb.protobuf.core.{CommandReply, CommandWrapper}
import lagompb.util.LagompbProtosCompanions
import org.slf4j.{Logger, LoggerFactory}

/**
  * LagomPbCommandSerializer
  * It is used internally by lagom-common to serialize commands and replies
  */
sealed class LagompbCommandSerde(val system: ExtendedActorSystem)
    extends SerializerWithStringManifest {

  private final val log: Logger =
    LoggerFactory.getLogger(classOf[LagompbCommandSerde])
  private val actorRefResolver = ActorRefResolver(system.toTyped)

  // construct a map of type_url -> companion object parser
  final lazy val msgMap: Map[String, Array[Byte] => scalapb.GeneratedMessage] =
    LagompbProtosCompanions.companions
      .map(
        companion =>
          (
            companion.scalaDescriptor.fullName,
            (s: Array[Byte]) => companion.parseFrom(s)
        )
      )
      .toMap

  final val LagomPbCommandManifest: String = classOf[LagompbCommand].getName

  override def manifest(o: AnyRef): String = o.getClass.getName

  override def toBinary(o: AnyRef): Array[Byte] = {
    o match {
      case LagompbCommand(cmd, actorRef, pluginData) =>
        val actorBytes: Array[Byte] = actorRefResolver
          .toSerializationFormat(actorRef)
          .getBytes(StandardCharsets.UTF_8)

        log.debug(
          s"serializing Command [${cmd.companion.scalaDescriptor.fullName}]"
        )

        CommandWrapper()
          .withCommand(Any.pack(cmd))
          .withActorRef(com.google.protobuf.ByteString.copyFrom(actorBytes))
          .withData(pluginData)
          .toByteArray

      case _ => throw new LagompbException("requires LagomPbCommand")
    }
  }

  override def identifier: Int = 5555

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    manifest match {
      case LagomPbCommandManifest =>
        val wrapper: CommandWrapper = CommandWrapper.parseFrom(bytes)
        val actorRefStr: String =
          new String(wrapper.actorRef.toByteArray, StandardCharsets.UTF_8)
        val ref: ActorRef[CommandReply] =
          actorRefResolver.resolveActorRef[CommandReply](actorRefStr)

        wrapper.command.fold(
          throw new LagompbException("requires LagompbCommand")
        )(any => {
          log.debug(s"deserializing Command #[${any.typeUrl}]")

          msgMap
            .get(any.typeUrl.split('/').lastOption.getOrElse(""))
            .fold(
              throw new LagompbException(
                s"unable to deserialize command ${any.typeUrl}. "
              )
            )(mesg => {

              val protoCmd: scalapb.GeneratedMessage =
                mesg(any.value.toByteArray)
              LagompbCommand(protoCmd, ref, wrapper.data)
            })
        })
      case _ => throw new LagompbException("Wrong LagompbCommand manifest....")
    }
}
