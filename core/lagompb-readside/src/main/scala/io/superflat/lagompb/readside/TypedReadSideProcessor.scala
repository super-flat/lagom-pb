package io.superflat.lagompb.readside

import akka.Done
import akka.actor.typed.ActorSystem
import io.superflat.lagompb.ProtosRegistry
import io.superflat.lagompb.encryption.EncryptionAdapter
import io.superflat.lagompb.protobuf.v1.core.MetaData
import scalapb.GeneratedMessage
import slick.dbio.{DBIO, DBIOAction}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
 * ReadSideProcessor that publishes to kafka
 *
 * @param encryptionAdapter EncryptionAdapter instance to use
 * @param actorSystem the actor system
 * @param ec the execution context
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
  final def handle(event: ReadSideEvent): DBIO[Done] =
    ProtosRegistry.unpackAnys(event.event, event.state) match {
      case Failure(e) =>
        DBIOAction.failed(e)
      case Success(messages) =>
        handleTyped(
          messages.head,
          event.eventTag,
          messages(1),
          event.metaData
        )
    }

  def handleTyped(
    event: GeneratedMessage,
    eventTag: String,
    state: GeneratedMessage,
    metaData: MetaData
  ): DBIO[Done]
}
