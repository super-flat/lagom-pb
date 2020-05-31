package lagompb.readside

import akka.Done
import com.github.ghik.silencer.silent
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import com.lightbend.lagom.scaladsl.persistence.slick.SlickReadSide
import com.typesafe.config.Config
import lagompb.{LagompbEvent, LagompbException}
import lagompb.protobuf.core.{EventWrapper, MetaData}
import lagompb.util.LagompbProtosCompanions
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}
import slick.dbio.{DBIO, DBIOAction}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/**
 * It will be implemented by any slick-based readSide Processor.
 * It must be registered in the LagompbApplication
 *
 * @param readSide the slick readSide component
 * @param config   the configuration instance
 * @tparam TState the aggregate state type
 */
@silent abstract class LagompbSlickRead[TState <: scalapb.GeneratedMessage](readSide: SlickReadSide, config: Config)(
    implicit ec: ExecutionContext
) extends ReadSideProcessor[LagompbEvent] {

  final override def buildHandler(): ReadSideProcessor.ReadSideHandler[LagompbEvent] =
    readSide
      .builder[LagompbEvent](readSideId)
      .setEventHandler[EventWrapper](eventStreamElement => {
        val eventStateWrapper: EventWrapper = eventStreamElement.event
        handle(eventStateWrapper)
      })
      .build()

  /**
   * Handles aggregate event persisted and made available for read model
   *
   * @param event the aggregate event
   * @param state the Lagompb state that wraps the actual state and some meta data
   */
  def handle(event: scalapb.GeneratedMessage, state: TState, metaData: MetaData): DBIO[Done]

  private def handle(eventWrapper: EventWrapper): DBIO[Done] = {
    LagompbProtosCompanions
      .getCompanion(eventWrapper.getEvent)
      .fold[DBIO[Done]](
        DBIOAction.failed(new LagompbException(s"[Lagompb] unable to parse event ${eventWrapper.getEvent.typeUrl}"))
      )((comp: GeneratedMessageCompanion[_ <: GeneratedMessage]) => {
        Try {
          handle(
            eventWrapper.getEvent.unpack(comp),
            eventWrapper.getResultingState
              .unpack[TState](aggregateStateCompanion),
            eventWrapper.getMeta
          )
        } match {
          case Failure(exception) =>
            DBIOAction.failed(exception)
          case Success(result: Any) =>
            result
        }
      })
  }

  final override def aggregateTags: Set[AggregateEventTag[LagompbEvent]] =
    LagompbEvent.Tag.allTags

  //  An identifier for this read side. This will be used to store offsets in the offset store.
  final def readSideId: String = config.getString("lagompb.service-name")

  /**
   * aggregate state. it is a generated scalapb message extending the LagompbState trait
   *
   * @return aggregate state
   */
  def aggregateStateCompanion: scalapb.GeneratedMessageCompanion[TState]

}
