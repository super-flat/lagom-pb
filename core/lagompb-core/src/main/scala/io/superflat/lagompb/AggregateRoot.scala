package io.superflat.lagompb

import java.time.Instant

import akka.actor.ActorSystem
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import com.google.protobuf.any.Any
import io.superflat.lagompb.encryption.EncryptionAdapter
import io.superflat.lagompb.protobuf.v1.core.CommandHandlerResponse.HandlerResponse.{
  Empty,
  FailedResponse,
  SuccessResponse
}
import io.superflat.lagompb.protobuf.v1.core.SuccessCommandHandlerResponse.Response.{Event, NoEvent}
import io.superflat.lagompb.protobuf.v1.core._
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}

/**
 * LagompbAggregate abstract class encapsulate all the necessary setup required to
 * create an aggregate in the lagom ecosystem. There are three main components an aggregate
 * requires to be functional: a commands handler, an events handler and a state.
 *
 * @param actorSystem     the underlying actor system
 * @param commandHandler  the commands handler
 * @param eventHandler    the events handler
 * @param encryptionAdapter optional ProtoEncryption implementation
 * @tparam S the scala type of the aggregate state
 */
abstract class AggregateRoot[S <: scalapb.GeneratedMessage](
  actorSystem: ActorSystem,
  commandHandler: CommandHandler,
  eventHandler: EventHandler,
  encryptionAdapter: EncryptionAdapter
) {

  final val log: Logger = LoggerFactory.getLogger(getClass)

  final val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command](aggregateName)

  /**
   * Defines the aggregate name. The `aggregateName` must be unique
   */
  def aggregateName: String

  /**
   * Defines the aggregate state.
   */
  def stateCompanion: scalapb.GeneratedMessageCompanion[S]

  final def create(entityContext: EntityContext[Command], shardIndex: Int): Behavior[Command] = {
    val persistenceId: PersistenceId =
      PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId)
    val selectedTag: String = ConfigReader.allEventTags(shardIndex)
    create(persistenceId)
      .withTagger(_ => Set(selectedTag))
      .withRetention(
        RetentionCriteria
          .snapshotEvery(
            numberOfEvents = ConfigReader.snapshotCriteria.frequency, // snapshotFrequency
            keepNSnapshots = ConfigReader.snapshotCriteria.retention //snapshotRetention
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
  ): EventSourcedBehavior[Command, EventWrapper, StateWrapper] = {
    val splitter: Char = PersistenceId.DefaultSeparator(0)
    val entityId: String = persistenceId.id.split(splitter).lastOption.getOrElse("")
    EventSourcedBehavior
      .withEnforcedReplies[Command, EventWrapper, StateWrapper](
        persistenceId = persistenceId,
        emptyState = initialState(entityId),
        commandHandler = genericCommandHandler,
        eventHandler = genericEventHandler
      )
  }

  private[lagompb] def initialState(entityId: String): StateWrapper =
    StateWrapper()
      .withState(Any.pack(stateCompanion.defaultInstance))
      .withMeta(MetaData.defaultInstance.withEntityId(entityId))

  /**
   * Encrypt the event to persist whenever an encryption is set.
   *
   * @param event the event to persist
   * @param state the resulting state
   * @param metaData the additional meta
   */
  private[lagompb] def encryptEvent(event: Any, state: Any, metaData: MetaData): (Any, Any, StateWrapper) = {
    val encryptedEvent: Any = encryptionAdapter.encryptOrThrow(event)

    // compute the resulting state once and reuse it
    val encryptedResultingState: Any = encryptionAdapter.encryptOrThrow(state)

    (encryptedEvent,
     encryptedResultingState,
     StateWrapper()
       .withState(state)
       .withMeta(metaData)
    )
  }

  /**
   * Call safely the event handler and return the resulting state when successful
   *
   * @param event the event to handlecompanion object of the event to handle
   * @param state the priorState to the event to handle
   * @param metaData the additional meta
   * @param replyTo the actor ref to reply to
   */
  private[lagompb] def persistEvent(event: Any,
                                    state: Any,
                                    metaData: MetaData,
                                    replyTo: ActorRef[CommandReply]
  ): ReplyEffect[EventWrapper, StateWrapper] =
    Try {
      eventHandler.handle(event, state, metaData)
    } match {
      case Failure(exception) =>
        log.error(s"event handler breakdown, ${exception.getMessage}", exception)

        Effect.reply(replyTo)(
          CommandReply()
            .withFailedReply(
              FailedReply()
                .withReason(
                  s"[Lagompb] EventHandler failure: ${exception.getMessage}"
                )
                .withCause(FailureCause.INTERNAL_ERROR)
            )
        )

      case Success(resultingState) =>
        log.debug(
          s"[Lagompb] user event handler returned ${resultingState.typeUrl}"
        )

        val (encryptedEvent, encryptedResultingState, decryptedStateWrapper) =
          encryptEvent(event, resultingState, metaData)

        Effect
          .persist(
            EventWrapper()
              .withEvent(encryptedEvent)
              .withResultingState(encryptedResultingState)
              .withMeta(metaData)
          )
          .thenReply(replyTo) { (_: StateWrapper) =>
            CommandReply()
              .withSuccessfulReply(
                SuccessfulReply()
                  // return decrypted state, not persisted (encrypted) state
                  .withStateWrapper(decryptedStateWrapper)
              )
          }
    }

  /**
   * unpacks the nested state in the event, throws away prior state
   *
   * @param priorState the current state
   * @param event      the event wrapper
   */
  private[lagompb] def genericEventHandler(priorState: StateWrapper, event: EventWrapper): StateWrapper =
    priorState.update(_.meta := event.getMeta, _.state := event.getResultingState)

  /**
   * Given a LagompbState implementation and a LagompbCommand, run the
   * implemented commandHandler.handle and persist/reply any event/state
   * as needed.
   *
   * @param stateWrapper state wrapper
   * @param cmd          the command to process
   */
  final def genericCommandHandler(stateWrapper: StateWrapper, cmd: Command): ReplyEffect[EventWrapper, StateWrapper] = {

    val maybeState: Try[Any] =
      // if no prior revisions, use default instance state
      if (stateWrapper.getMeta.revisionNumber == 0L)
        Try(stateWrapper.getState)
      else
        // otherwise attempt to decrypt prior state
        encryptionAdapter.decrypt(stateWrapper.getState)

    maybeState match {

      case Failure(exception) =>
        val errMsg: String = s"state parser failure, ${exception.getMessage}"
        log.error(errMsg, exception)

        throw new GlobalException(errMsg)

      case Success(decryptedState) =>
        log.debug(s"[Lagompb] plugin data ${cmd.data} is valid...")

        commandHandler.handle(cmd.command, decryptedState, stateWrapper.getMeta) match {

          case Success(commandHandlerResponse: CommandHandlerResponse) =>
            commandHandlerResponse.handlerResponse match {

              // A successful response is returned by
              // the command handler
              case SuccessResponse(successResponse) =>
                successResponse.response match {

                  // No event to persist
                  case NoEvent(_) =>
                    // create a state wrapper with the decrypted event
                    val decryptedStateWrapper: StateWrapper = stateWrapper
                      .withState(decryptedState)

                    Effect.reply(cmd.replyTo)(
                      CommandReply()
                        .withSuccessfulReply(
                          SuccessfulReply()
                            .withStateWrapper(decryptedStateWrapper)
                        )
                    )

                  // Some event to persist
                  case Event(event: Any) =>
                    // let us construct the event meta prior to call the user agent
                    val eventMeta: MetaData = MetaData()
                      .withRevisionNumber(stateWrapper.getMeta.revisionNumber + 1)
                      .withRevisionDate(Instant.now().toTimestamp)
                      .withData(cmd.data)
                      // the priorState will always have the entityId set in its meta even for the initial state
                      .withEntityId(stateWrapper.getMeta.entityId)

                    persistEvent(event, decryptedState, eventMeta, cmd.replyTo)

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
                            .withCause(FailureCause.VALIDATION_ERROR)
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

              // An unhandled response is returned by the command handler
              case Empty =>
                Effect.reply(cmd.replyTo)(
                  CommandReply()
                    .withFailedReply(
                      FailedReply()
                        .withReason(
                          s"unknown command handler response ${commandHandlerResponse.handlerResponse.getClass.getName}"
                        )
                        .withCause(FailureCause.VALIDATION_ERROR)
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
