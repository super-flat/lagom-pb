package lagompb

import java.time.Instant

import akka.actor.ActorSystem
import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import com.google.protobuf.any.Any
import lagompb.core._
import lagompb.core.CommandHandlerResponse.HandlerResponse.{Empty, FailedResponse, SuccessResponse}
import lagompb.core.SuccessCommandHandlerResponse.Response.{Event, NoEvent}
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}

/**
 * LagompbAggregate abstract class encapsulate all the necessary setup required to
 * create an aggregate in the lagom ecosystem. There are three main components an aggregate
 * requires to be functional: a commands handler, an events handler and a state.
 *
 * @param actorSystem    the underlying actor system
 * @param commandHandler the commands handler
 * @param eventHandler   the events handler
 * @tparam TState the scala type of the aggregate state
 */
abstract class LagompbAggregate[TState <: scalapb.GeneratedMessage](
    actorSystem: ActorSystem,
    commandHandler: LagompbCommandHandler[TState],
    eventHandler: LagompbEventHandler[TState]
) {

  final val log: Logger = LoggerFactory.getLogger(getClass)

  final val typeKey: EntityTypeKey[LagompbCommand] =
    EntityTypeKey[LagompbCommand](aggregateName)

  /**
   * Defines the aggregate name. The `aggregateName` must be unique
   */
  def aggregateName: String

  /**
   * Defines the aggregate state.
   */
  def stateCompanion: scalapb.GeneratedMessageCompanion[TState]

  final def create(entityContext: EntityContext[LagompbCommand], shardIndex: Int): Behavior[LagompbCommand] = {
    val persistenceId: PersistenceId =
      PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId)
    val selectedTag: String = LagompbConfig.allEventTags(shardIndex)
    create(persistenceId)
      .withTagger(_ => Set(selectedTag))
      .withRetention(
        RetentionCriteria
          .snapshotEvery(
            numberOfEvents = LagompbConfig.snapshotCriteria.frequency, // snapshotFrequency
            keepNSnapshots = LagompbConfig.snapshotCriteria.retention //snapshotRetention
          )
      )
  }

  /**
   * Returns the EventSourcedBehavior for lagom, wiring together the
   * user-provided LagompbCommandHandler and LagompbEventHandler
   *
   * @param persistenceId the aggregate persistence Id
   */
  private[lagompb] def create(
      persistenceId: PersistenceId
  ): EventSourcedBehavior[LagompbCommand, EventWrapper, StateWrapper] =
    EventSourcedBehavior
      .withEnforcedReplies[LagompbCommand, EventWrapper, StateWrapper](
        persistenceId = persistenceId,
        emptyState = initialState,
        commandHandler = genericCommandHandler,
        eventHandler = genericEventHandler
      )

  private def initialState: StateWrapper =
    StateWrapper()
      .withState(Any.pack(stateCompanion.defaultInstance))
      .withMeta(MetaData.defaultInstance)

  /**
   * unpacks the nested state in the event, throws away prior state
   *
   * @param priorState the current state
   * @param event      the event wrapper
   */
  private[lagompb] def genericEventHandler(priorState: StateWrapper, event: EventWrapper): StateWrapper = {
    priorState.update(_.meta := event.getMeta, _.state := event.getResultingState)
  }

  /**
   * Given a LagompbState implementation and a LagompbCommand, run the
   * implemented commandHandler.handle and persist/reply any event/state
   * as needed.
   *
   * @param stateWrapper state wrapper
   * @param cmd          the command to process
   */
  final def genericCommandHandler(
      stateWrapper: StateWrapper,
      cmd: LagompbCommand
  ): ReplyEffect[EventWrapper, StateWrapper] = {

    // parse nested state
    Try {
      stateWrapper.getState.unpack[TState](stateCompanion)
    } match {
      case Failure(exception) =>
        val errMsg: String = s"state parser failure, ${exception.getMessage}"
        log.error(errMsg, exception)

        throw new LagompbException(errMsg)

      case Success(state) =>
        log.debug(s"[LagomPb] plugin data ${cmd.data} is valid...")

        commandHandler.handle(cmd, state, stateWrapper.getMeta) match {

          case Success(commandHandlerResponse: CommandHandlerResponse) =>
            commandHandlerResponse.handlerResponse match {

              // A successful response is returned by
              // the command handler
              case SuccessResponse(successResponse) =>
                successResponse.response match {

                  // No event to persist
                  case NoEvent(_) =>
                    Effect.reply(cmd.replyTo)(
                      CommandReply()
                        .withSuccessfulReply(
                          SuccessfulReply()
                            .withStateWrapper(stateWrapper)
                        )
                    )

                  // Some event to persist
                  case Event(event: Any) =>
                    LagompbProtosRegistry
                      .getCompanion(event)
                      .fold[ReplyEffect[EventWrapper, StateWrapper]](
                        Effect.reply(cmd.replyTo)(
                          CommandReply()
                            .withFailedReply(
                              FailedReply()
                                .withReason(
                                  s"[Lagompb] unable to parse event ${event.typeUrl} emitted by the command handler"
                                )
                                .withCause(FailureCause.InternalError)
                            )
                        )
                      )(comp => {
                        // let us construct the event meta prior to call the user agent
                        val eventMeta: MetaData = MetaData()
                          .withRevisionNumber(stateWrapper.getMeta.revisionNumber + 1)
                          .withRevisionDate(Instant.now().toTimestamp)
                          .withData(cmd.data)

                        // let us the event handler
                        val resultingState: TState = eventHandler
                          .handle(event.unpack(comp), state, eventMeta)

                        log.debug(
                          s"[Lagompb] user defined event handler called with resulting state ${resultingState.companion.scalaDescriptor.fullName}"
                        )

                        Effect
                          .persist(
                            EventWrapper()
                              .withEvent(event)
                              .withResultingState(Any.pack(resultingState))
                              .withMeta(eventMeta)
                          )
                          .thenReply(cmd.replyTo)((updatedStateWrapper: StateWrapper) => {
                            CommandReply()
                              .withSuccessfulReply(
                                SuccessfulReply()
                                  .withStateWrapper(updatedStateWrapper)
                              )
                          })
                      })

                  // the command handler return some unhandled successful response
                  // this case may never happen but it is safe to always check it
                  case _ =>
                    Effect.reply(cmd.replyTo)(
                      CommandReply()
                        .withFailedReply(
                          FailedReply()
                            .withReason(
                              s"unknown command handler success response ${successResponse.response.getClass.getName}"
                            )
                            .withCause(FailureCause.ValidationError)
                        )
                    )
                }

              // A failed response is returned by the
              // command handler
              case FailedResponse(failedResponse) =>
                Effect.reply(cmd.replyTo)(
                  CommandReply()
                    .withFailedReply(
                      FailedReply()
                        .withReason(failedResponse.reason)
                        .withCause(failedResponse.cause)
                    )
                )

              // An unhandled response is returned by the comand handler
              case Empty =>
                Effect.reply(cmd.replyTo)(
                  CommandReply()
                    .withFailedReply(
                      FailedReply()
                        .withReason(
                          s"unknown command handler response ${commandHandlerResponse.handlerResponse.getClass.getName}"
                        )
                        .withCause(FailureCause.ValidationError)
                    )
                )
            }
          case Failure(exception) =>
            val errMsg: String =
              s"command handler breakdown, ${exception.getMessage}"
            log.error(errMsg, exception)
            throw exception
        }
    }
  }
}
