package io.superflat.lagompb.readside

import akka.Done
import akka.projection.eventsourced.EventEnvelope
import akka.projection.slick.SlickHandler
import io.superflat.lagompb.GlobalException
import io.superflat.lagompb.encryption.{DecryptPermanentFailure, EncryptionAdapter}
import io.superflat.lagompb.protobuf.v1.core.EventWrapper
import org.slf4j.{Logger, LoggerFactory}
import slick.dbio.{DBIO, DBIOAction}

import scala.util.{Failure, Success, Try}

/**
 * Reads the journal events, runs provided eventProcessor, and
 * presists the offset into postgres
 *
 * @param eventTag the event tag read
 * @param eventProcessor the actual event processor
 * @param encryptionAdapter handles encrypt/decrypt transformations
 */
final class EventsReader(eventTag: String, eventProcessor: EventProcessor, encryptionAdapter: EncryptionAdapter)
    extends SlickHandler[EventEnvelope[EventWrapper]] {

  val log: Logger = LoggerFactory.getLogger(getClass)

  /**
   * Processes events from the Journal by wrapping them in an envelope
   *
   * @param envelope the event envelope
   * @return
   */
  override def process(envelope: EventEnvelope[EventWrapper]): DBIO[Done] = {
    // decrypt the event/state as needed
    encryptionAdapter
      .decryptEventWrapper(envelope.event)
      .map({
        case EventWrapper(Some(event), Some(resultingState), Some(meta), _) =>
          eventProcessor.process(event, eventTag, resultingState, meta)

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
}
