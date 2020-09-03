package io.superflat.lagompb.readside

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import com.google.protobuf.any.Any
import slick.dbio.{DBIO, DBIOAction}
import io.superflat.lagompb.ProtosRegistry
import io.superflat.lagompb.encryption.EncryptionAdapter
import io.superflat.lagompb.protobuf.v1.core.{MetaData}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext

/**
 * ReadSideProcessor that publishes to kafka
 *
 * @param encryptionAdapter EncryptionAdapter instance to use
 * @param actorSystem the actor system
 * @param ec the execution context
 * @tparam S the aggregate state type
 */

abstract class TypedReadSideProcessor(encryptionAdapter: EncryptionAdapter)(implicit
  ec: ExecutionContext,
  actorSystem: ActorSystem[_]
) extends ReadSideProcessor(encryptionAdapter) {

  /**
   * Handles aggregate event persisted and made available for read model
   *
   * @param event the aggregate event
   */
  def handle(event: ReadSideEvent): DBIO[Done] = {
    ProtosRegistry.unpackAnys(event.event, event.state) match {
      case Failure(e) =>
        throw e
      case Success(messages) =>
        handleTyped(
          messages(0),
          event.eventTag,
          messages(1),
          event.metaData
        )
    }
  }

  def handleTyped(
    event: GeneratedMessage,
    eventTag: String,
    state: GeneratedMessage,
    metaData: MetaData
  ): DBIO[Done]
}
