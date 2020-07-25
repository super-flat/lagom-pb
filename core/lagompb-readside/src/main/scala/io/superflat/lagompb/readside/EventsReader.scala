package io.superflat.lagompb.readside

import akka.Done
import akka.projection.eventsourced.EventEnvelope
import akka.projection.slick.SlickHandler
import com.google.protobuf.any
import io.superflat.lagompb.{GlobalException, ProtosRegistry}
import io.superflat.lagompb.encryption.{DecryptPermanentFailure, ProtoEncryption}
import io.superflat.lagompb.protobuf.core.EventWrapper
import io.superflat.lagompb.protobuf.encryption.EncryptedProto
import org.slf4j.{Logger, LoggerFactory}
import slick.dbio.{DBIO, DBIOAction}

import scala.util.{Failure, Success, Try}

/**
 * Reads the journal events by persisting the read offset onto postgres
 *
 * @param eventTag the event tag read
 * @param encryption the event read
 * @param eventProcessor the actual event processor
 */
final class EventsReader(eventTag: String, encryption: ProtoEncryption, eventProcessor: EventProcessor)
    extends SlickHandler[EventEnvelope[EncryptedProto]] {

  final val log: Logger = LoggerFactory.getLogger(getClass)

  /**
   * Processes events from the Journal by wrapping them in an envelope
   *
   * @param envelope the event envelope
   * @return
   */
  final override def process(envelope: EventEnvelope[EncryptedProto]): DBIO[Done] =
    encryption
      // decrypt the message into an Any
      .decrypt(envelope.event)
      // unpack into the EventWrapper
      .map(_.unpack(EventWrapper))
      // handle happy path decryption
      .map({
        case EventWrapper(Some(event: any.Any), Some(resultingState), Some(meta)) =>
          ProtosRegistry.companion(event) match {
            case Some(comp) =>
              eventProcessor
                .process(comp, event, eventTag, resultingState, meta)

            case None =>
              DBIOAction.failed(new GlobalException(s"companion not found for ${event.typeUrl}"))
          }

        case _ =>
          DBIO.failed(new GlobalException(s"[Lagompb] unknown event received ${envelope.event.getClass.getName}"))
      })
      .recoverWith({
        case DecryptPermanentFailure(reason) =>
          log.debug(s"skipping offset with reason, $reason")
          Try(DBIOAction.successful(Done))

        case throwable: Throwable =>
          log.error("failed to handle event", throwable)
          Try(DBIO.failed(throwable))
      }) match {
      case Success(value)     => value
      case Failure(exception) => throw exception
    }
}
