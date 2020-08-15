package io.superflat.lagompb

import akka.actor.typed.ActorRef
import io.superflat.lagompb.protobuf.v1.core.CommandReply

/**
 * Defines the type of command to handle by the aggregate.
 * `LagomPbCommand` is a wrapper around the actual command sent to aggregate.
 *
 * @param command the actual command. This is a protocol buffer message
 * @param replyTo the actor reference of the sender of the command.
 * @param data additional data that will be added to the state. This may be deprecated when
 *                    the plugin architecture is in placed
 * The CommandReply message type will be sent back that actor reference
 */
final case class Command(command: scalapb.GeneratedMessage, replyTo: ActorRef[CommandReply], data: Map[String, String])
