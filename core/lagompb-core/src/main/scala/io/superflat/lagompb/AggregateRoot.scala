package io.superflat.lagompb

import java.time.Instant

import akka.actor.ActorSystem
import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import com.google.protobuf.any.Any
import io.superflat.lagompb.encryption.{
  EncryptedEventAdapter,
  EncryptedSnapshotAdapter,
  EncryptionAdapter,
  ProtoEncryption
}
import io.superflat.lagompb.protobuf.core._
import io.superflat.lagompb.protobuf.core.CommandHandlerResponse.HandlerResponse.{
  Empty,
  FailedResponse,
  SuccessResponse
}
import io.superflat.lagompb.protobuf.core.SuccessCommandHandlerResponse.Response.{Event, NoEvent}
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
 * @param protoEncryption optional ProtoEncryption implementation
 * @tparam S the scala type of the aggregate state
 */
abstract class AggregateRoot[S <: scalapb.GeneratedMessage](
  actorSystem: ActorSystem,
  commandHandler: CommandHandler[S],
  eventHandler: EventHandler[S],
  protoEncryption: Option[ProtoEncryption] = None
) {

  final val log: Logger = LoggerFactory.getLogger(getClass)

  final val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command](aggregateName)

  final val encryptionAdapter: EncryptionAdapter = new EncryptionAdapter(protoEncryption)

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
    val entityId = persistenceId.id.split(splitter).lastOption.getOrElse("")
    EventSourcedBehavior
      .withEnforcedReplies[Command, EventWrapper, StateWrapper](
        persistenceId = persistenceId,
        emptyState = initialState(entityId),
        commandHandler = genericCommandHandler,
        eventHandler = genericEventHandler
      )
  }

  private[this] def initialState(entityId: String): StateWrapper =
    StateWrapper()
      .withState(Any.pack(stateCompanion.defaultInstance))
      .withMeta(MetaData.defaultInstance.withEntityId(entityId))

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

    encryptionAdapter
      .decrypt(stateWrapper.getState)
      .map(_.unpack[S](stateCompanion)) match {

      case Failure(exception) =>
        val errMsg: String = s"state parser failure, ${exception.getMessage}"
        log.error(errMsg, exception)

        throw new GlobalException(errMsg)

      case Success(decryptedState) =>
        log.debug(s"[Lagompb] plugin data ${cmd.data} is valid...")

        commandHandler.handle(cmd, decryptedState, stateWrapper.getMeta) match {

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
                      .withState(Any.pack(decryptedState))

                    Effect.reply(cmd.replyTo)(
                      CommandReply()
                        .withSuccessfulReply(
                          SuccessfulReply()
                            .withStateWrapper(decryptedStateWrapper)
                        )
                    )

                  // Some event to persist
                  case Event(event: Any) =>
                    // get the companion for provided event
                    ProtosRegistry.companion(event) match {
                      // if no companion, return failure
                      case None =>
                        Effect.reply(cmd.replyTo)(
                          CommandReply().withFailedReply(
                            FailedReply()
                              .withReason(
                                s"[Lagompb] unable to parse event ${event.typeUrl} emitted by the command handler"
                              )
                              .withCause(FailureCause.InternalError)
                          )
                        )

                      // otherwise persist the event and reply with state
                      case Some(comp) =>
                        // let us construct the event meta prior to call the user agent
                        val eventMeta: MetaData = MetaData()
                          .withRevisionNumber(stateWrapper.getMeta.revisionNumber + 1)
                          .withRevisionDate(Instant.now().toTimestamp)
                          .withData(cmd.data)
                          // the priorState will always have the entityId set in its meta even for the initial state
                          .withEntityId(stateWrapper.getMeta.entityId)

                        // let us the event handler
                        val resultingState: S = eventHandler
                          .handle(event.unpack(comp), decryptedState, eventMeta)

                        log.debug(
                          s"[Lagompb] user event handler returned ${resultingState.companion.scalaDescriptor.fullName}"
                        )

                        val encryptedEvent = encryptionAdapter.encrypt(event).get
                        val anyResultingState: Any = Any.pack(resultingState)
                        val encryptedResultingState = encryptionAdapter.encrypt(anyResultingState).get
                        val decryptedStateWrapper = StateWrapper()
                          .withState(anyResultingState)
                          .withMeta(eventMeta)

                        Effect
                          .persist(
                            EventWrapper()
                              .withEvent(encryptedEvent)
                              .withResultingState(encryptedResultingState)
                              .withMeta(eventMeta)
                          )
                          .thenReply(cmd.replyTo) { (updatedStateWrapper: StateWrapper) =>
                            CommandReply()
                              .withSuccessfulReply(
                                SuccessfulReply()
                                  // TODO make this return decrypted state!
                                  .withStateWrapper(updatedStateWrapper)
                              )
                          }
                    }

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

              // An unhandled response is returned by the command handler
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
