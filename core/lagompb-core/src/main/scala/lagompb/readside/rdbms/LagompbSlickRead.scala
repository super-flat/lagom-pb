package lagompb.readside.rdbms

import akka.Done
import com.github.ghik.silencer.silent
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.slick.SlickReadSide
import com.typesafe.config.Config
import lagompb.LagompbEvent
import lagompb.LagompbException
import lagompb.LagompbState
import lagompb.protobuf.core.EventWrapper
import lagompb.util.LagompbProtosCompanions
import scalapb.GeneratedMessage
import scalapb.GeneratedMessageCompanion
import slick.dbio.DBIO
import slick.dbio.DBIOAction

import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success
import scala.util.Try

/**
 * It will be implemented by any slick-based readSide Processor.
 * It must be registered in the [[lagompb.LagompbApplication]]
 *
 * @param readSide the slick readSide component
 * @param config   the type configuration
 * @tparam TState the aggregate state type
 */
@silent abstract class LagompbSlickRead[TState <: scalapb.GeneratedMessage](
    readSide: SlickReadSide,
    config: Config,
)(implicit ec: ExecutionContext)
    extends ReadSideProcessor[LagompbEvent] {

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
  def handle(
      event: scalapb.GeneratedMessage,
      state: LagompbState[TState]
  ): DBIO[Done]

  private def handle(eventWrapper: EventWrapper): DBIO[Done] = {
    LagompbProtosCompanions
      .getCompanion(eventWrapper.getEvent)
      .fold[DBIO[Done]](
        DBIOAction.failed(new LagompbException(s"[Lagompb] unable to parse event ${eventWrapper.getEvent.typeUrl}"))
      )((comp: GeneratedMessageCompanion[_ <: GeneratedMessage]) => {
        Try {

          handle(
            eventWrapper.getEvent.unpack(comp),
            LagompbState[TState](
              eventWrapper.getResultingState.unpack[TState](aggregateStateCompanion),
              eventWrapper.getMeta
            )
          )
        } match {
          case Failure(exception) =>
            DBIOAction.failed(exception)
          case Success(result: Any) =>
            result
        }
      })
  }

  final override def aggregateTags: Set[AggregateEventTag[LagompbEvent]] = LagompbEvent.Tag.allTags

  //  An identifier for this read side. This will be used to store offsets in the offset store.
  final def readSideId: String = config.getString("lagompb.service-name")

  /**
   * aggregate state. it is a generated scalapb message extending the LagompbState trait
   *
   * @return aggregate state
   */
  def aggregateStateCompanion: scalapb.GeneratedMessageCompanion[TState]

}
