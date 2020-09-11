package io.superflat.lagompb

import java.time.Instant

import akka.actor.ActorSystem
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import com.google.protobuf.any.Any
import io.superflat.lagompb.encryption.EncryptionAdapter
import io.superflat.lagompb.protobuf.v1.core.CommandHandlerResponse.Response
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
 * @param initialState    the aggregate initial state.
 *                        Note: The type of the state must be the same as define as type parameter
 *                        in both commands and events handler when using the [[io.superflat.lagompb.TypedCommandHandler]] and
 *                        [[io.superflat.lagompb.TypedEventHandler]]
 * @param encryptionAdapter optional ProtoEncryption implementatione scala type of the aggregate state
 */
abstract class AggregateRoot(
    actorSystem: ActorSystem,
    commandHandler: CommandHandler,
    eventHandler: EventHandler,
    initialState: scalapb.GeneratedMessage,
    encryptionAdapter: EncryptionAdapter
) {

  final val log: Logger = LoggerFactory.getLogger(getClass)

  final val typeKey: EntityTypeKey[Command] =
    EntityTypeKey[Command](aggregateName)

  /**
   * Defines the aggregate name. The `aggregateName` must be unique
   */
  def aggregateName: String

  final def create(
      entityContext: EntityContext[Command],
      shardIndex: Int
  ): Behavior[Command] = {
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
    val entityId: String =
      persistenceId.id.split(splitter).lastOption.getOrElse("")
    EventSourcedBehavior
      .withEnforcedReplies[Command, EventWrapper, StateWrapper](
        persistenceId = persistenceId,
        emptyState = initialState(entityId),
        commandHandler = genericCommandHandler,
        eventHandler = genericEventHandler
      )
  }

  private[lagompb] def initialState(entityId: String): StateWrapper = {
    StateWrapper()
      .withState(Any.pack(initialState))
      .withMeta(MetaData.defaultInstance.withEntityId(entityId))
  }

  /**
   * Encrypt the event to persist whenever an encryption is set.
   *
   * @param event the event to persist
   * @param state the resulting state
   * @param metaData the additional meta
   */
  private[lagompb] def encryptEvent(
      event: Any,
      state: Any,
      metaData: MetaData
  ): (Any, Any, StateWrapper) = {
    (
      encryptionAdapter.encryptOrThrow(event),
      // compute the resulting state once and reuse it
      encryptionAdapter.encryptOrThrow(state),
      StateWrapper().withState(state).withMeta(metaData)
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
  private[lagompb] def persistEventAndReply(
      event: Any,
      state: Any,
      metaData: MetaData,
      replyTo: ActorRef[CommandReply]
  ): ReplyEffect[EventWrapper, StateWrapper] = {
    Try {
      eventHandler.handle(event, state, metaData)
    } match {
      case Failure(exception: Throwable) =>
        Effect.reply(replyTo)(
          CommandReply().withFailure(
            FailureResponse().withCritical(s"[Lagompb] EventHandler failure:, ${exception.getMessage}")
          )
        )

      case Success(resultingState) =>
        log.debug(s"[Lagompb] Event handler returned ${resultingState.typeUrl}")

        val (encryptedEvent, encryptedResultingState, decryptedStateWrapper) =
          encryptEvent(event, resultingState, metaData)

        Effect
          .persist(
            EventWrapper().withEvent(encryptedEvent).withResultingState(encryptedResultingState).withMeta(metaData)
          )
          .thenReply(replyTo)((_: StateWrapper) => CommandReply().withStateWrapper(decryptedStateWrapper))
    }
  }

  /**
   * Set event/state metadata given the current StateWrapper and the additonal meta data
   * and return the new metadata
   *
   * @param stateWrapper the current StateWrapper
   * @param data the addional data
   * @return newly created MetaData
   */
  private[lagompb] def nextMeta(
      stateWrapper: StateWrapper,
      data: Map[String, String]
  ): MetaData = {
    MetaData()
      .withRevisionNumber(stateWrapper.getMeta.revisionNumber + 1)
      .withRevisionDate(Instant.now().toTimestamp)
      .withData(data)
      .withEntityId(stateWrapper.getMeta.entityId)
  }

  /**
   * unpacks the nested state in the event, throws away prior state
   *
   * @param priorState the current state
   * @param event      the event wrapper
   */
  private[lagompb] def genericEventHandler(
      priorState: StateWrapper,
      event: EventWrapper
  ): StateWrapper = {
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
      cmd: Command
  ): ReplyEffect[EventWrapper, StateWrapper] = {

    val maybeState: Try[Any] =
      // if no prior revisions, use default instance state
      if (stateWrapper.getMeta.revisionNumber == 0L)
        Try(stateWrapper.getState)
      else
        // otherwise attempt to decrypt prior state
        encryptionAdapter.decrypt(stateWrapper.getState)

    maybeState match {
      case Failure(exception: Throwable) =>
        Effect.reply(cmd.replyTo)(
          CommandReply().withFailure(
            FailureResponse().withCritical(s"[Lagompb] state parser failure, ${exception.getMessage}")
          )
        )

      case Success(decryptedState: Any) =>
        log.debug(s"[Lagompb] plugin data ${cmd.data} is valid...")

        commandHandler.handle(cmd.command, decryptedState, stateWrapper.getMeta) match {

          case Success(commandHandlerResponse: CommandHandlerResponse) =>
            commandHandlerResponse.response match {
              case Response.Event(event: Any) =>
                persistEventAndReply(event, decryptedState, nextMeta(stateWrapper, cmd.data), cmd.replyTo)

              case Response.Failure(failure: FailureResponse) =>
                Effect.reply(cmd.replyTo)(CommandReply().withFailure(failure))

              case Response.Empty =>
                Effect.reply(cmd.replyTo)(CommandReply().withStateWrapper(stateWrapper.withState(decryptedState)))
            }

          case Failure(exception: Throwable) =>
            Effect.reply(cmd.replyTo)(
              CommandReply().withFailure(
                FailureResponse().withCritical(s"[Lagompb] command handler failure: ${exception.getMessage}")
              )
            )
        }
    }
  }
}
