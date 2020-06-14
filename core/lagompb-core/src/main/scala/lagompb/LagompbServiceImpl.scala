package lagompb

import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.grpc.GrpcServiceException
import akka.util.Timeout
import com.google.protobuf.any.Any
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import io.grpc.Status
import lagompb.core._
import lagompb.core.CommandReply.Reply
import lagompb.extensions.ExtensionsProto
import scalapb.GeneratedMessage

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

sealed trait LagompbBaseServiceImpl {

  implicit val timeout: Timeout = LagompbConfig.askTimeout

  /**
   * aggregateRoot defines the persistent entity that will be used to handle commands
   *
   * @see [[lagompb.LagompbAggregate]].
   *      Also for more info refer to the lagom doc [[https://www.lagomframework.com/documentation/1.6.x/scala/UsingAkkaPersistenceTyped.html]]
   */
  def aggregateRoot: LagompbAggregate[_]

  /**
   * aggregate state. it is a generated scalapb message extending the LagompbState trait
   *
   * @return aggregate state
   */
  def aggregateStateCompanion: scalapb.GeneratedMessageCompanion[_ <: scalapb.GeneratedMessage]

  /**
   * sends commands to the aggregate root and return a future of the aggregate state given a entity Id
   * the given entity Id is obtained the cluster shard.
   *
   * @param entityUuid the entity Id added or retrieved from the shard
   * @param cmd        the command to send to the aggregate. It is a scalapb generated case class from the command
   *                   protocol buffer message definition
   * @param data       additional data that need to be set in the state meta
   * @tparam TCommand the Type of the command to send.
   * @return Future of state
   */
  def sendCommand[TCommand <: scalapb.GeneratedMessage, TState <: scalapb.GeneratedMessage](
      clusterSharding: ClusterSharding,
      entityUuid: String,
      cmd: TCommand,
      data: Map[String, String]
  )(implicit ec: ExecutionContext, timeout: Timeout): Future[LagompbState[TState]] = {
    clusterSharding
      .entityRefFor(aggregateRoot.typeKey, entityUuid)
      .ask[CommandReply](replyTo => LagompbCommand(cmd, replyTo, data))
      .map((value: CommandReply) => handleLagompbCommandReply[TState](value))
  }

  private[lagompb] def handleLagompbCommandReply[TState <: scalapb.GeneratedMessage](
      commandReply: CommandReply
  ): LagompbState[TState] = {
    commandReply.reply match {
      case Reply.SuccessfulReply(successReply) =>
        parseState[TState](successReply.getStateWrapper)
      case Reply.FailedReply(failureReply) =>
        failureReply.cause match {
          case FailureCause.ValidationError =>
            throw new LagompbInvalidCommandException(failureReply.reason)
          case FailureCause.InternalError =>
            throw new LagompbException(failureReply.reason)
          case _ => throw new LagompbException("reason unknown")
        }
      case _ =>
        throw new LagompbException(s"unknown LagompbCommandReply ${commandReply.reply.getClass.getName}")
    }
  }

  private[lagompb] def parseState[TState <: scalapb.GeneratedMessage](
      stateWrapper: StateWrapper
  ): LagompbState[TState] = {
    val meta: MetaData = stateWrapper.getMeta
    val state: Any = stateWrapper.getState
    val parsed: TState = parseAny[TState](state)
    LagompbState[TState](parsed, meta)
  }

  private[lagompb] def parseAny[TState <: scalapb.GeneratedMessage](data: Any): TState = {
    val typeUrl: String = data.typeUrl.split('/').lastOption.getOrElse("")

    if (aggregateStateCompanion.scalaDescriptor.fullName.equals(typeUrl)) {
      Try {
        data.unpack(aggregateStateCompanion).asInstanceOf[TState]
      } match {
        case Failure(exception) =>
          throw new LagompbException(exception.getMessage)
        case Success(value) => value
      }
    } else throw new LagompbException("wrong state definition")
  }
}

/**
 * LagompbRestServiceImpl for the REST api implementation
 *
 * @param clusterSharding          the cluster sharding
 * @param persistentEntityRegistry the persistence entity registry
 * @param ec                       the execution context
 */
sealed abstract class LagompbRestServiceImpl(
    clusterSharding: ClusterSharding,
    persistentEntityRegistry: PersistentEntityRegistry
)(implicit ec: ExecutionContext, timeout: Timeout)
    extends LagompbBaseServiceImpl {

  /**
   * Sends command to the aggregate root. The command must have the aggregate entity id set.
   * When the entity id is not set a BadRequest is sent to the api
   *
   * @param cmd  the command to send
   * @param data the additional data to send
   * @tparam TCommand the command scala type
   * @tparam TState   the actual state scala type
   * @return the [[lagompb.LagompbState]] containing the actual state and the event meta
   */
  final def sendCommand[TCommand <: GeneratedMessage, TState <: scalapb.GeneratedMessage](
      cmd: TCommand,
      data: Map[String, String] = Map.empty
  ): Future[LagompbState[TState]] = {

    cmd.companion.scalaDescriptor.fields
      .find(field => field.getOptions.extension(ExtensionsProto.command).exists(_.entityId))
      .fold[Future[LagompbState[TState]]](Future.failed(BadRequest("command does not have entity key set.")))(fd => {
        val entityId: String = cmd.getField(fd).as[String]
        super
          .sendCommand[TCommand, TState](clusterSharding, entityId, cmd, data)
          .transform {
            case Failure(exception) =>
              exception match {
                case e: LagompbException =>
                  Failure(InternalServerError(e.getMessage))
                case e: LagompbInvalidCommandException =>
                  Failure(BadRequest(e.getMessage))
                case _ => Failure(InternalServerError(""))
              }
            case Success(value) => Success(value)
          }
      })
  }

  /**
   * Sends command to the aggregate root. The command must have the aggregate entity id set.
   * When the entity id is not set a BadRequest is sent to the api
   *
   * @param entityId the entity ID
   * @param cmd      the command to send
   * @param data     the additional data to send
   * @tparam TCommand the command scala type
   * @tparam TState   the actual state scala type
   * @return the [[lagompb.LagompbState]] containing the actual state and the event meta
   */
  final def sendCommand[TCommand <: GeneratedMessage, TState <: scalapb.GeneratedMessage](
      entityId: String,
      cmd: TCommand,
      data: Map[String, String]
  ): Future[LagompbState[TState]] = {
    super
      .sendCommand[TCommand, TState](clusterSharding, entityId, cmd, data)
      .transform {
        case Failure(exception) =>
          exception match {
            case e: LagompbException =>
              Failure(InternalServerError(e.getMessage))
            case e: LagompbInvalidCommandException =>
              Failure(BadRequest(e.getMessage))
            case _ => Failure(InternalServerError(""))
          }
        case Success(value) => Success(value)
      }
  }
}

/**
 * LagompbServiceImplWithKafka trait.
 *
 * It must be implemented by any lagom REST service with message broker features.
 * Automatically the domain events are wrapped in a ServiceEvent and push to a kafka topic
 * created automatically when not yet created.
 *
 * @param clusterSharding          the cluster sharding
 * @param persistentEntityRegistry the persistence entity registry
 * @param ec                       the execution context
 */
abstract class LagompbServiceImplWithKafka(
    val clusterSharding: ClusterSharding,
    val persistentEntityRegistry: PersistentEntityRegistry,
    val aggregate: LagompbAggregate[_]
)(implicit ec: ExecutionContext, timeout: Timeout)
    extends LagompbRestServiceImpl(clusterSharding, persistentEntityRegistry)(ec, timeout)
    with LagompbServiceWithKafka {

  final override def aggregateRoot: LagompbAggregate[_] = aggregate

  /** handle ServiceEvent topic
   *
   * @return the ServiceEvent topic handler
   */
  final override def kafkaEvents: Topic[KafkaEvent] =
    TopicProducer.taggedStreamWithOffset(LagompbEvent.Tag) { (tag, fromOffset) =>
      persistentEntityRegistry
        .eventStream(tag, fromOffset)
        .map(ev => (transformEvent(ev), ev.offset))
    }

  private def transformEvent(ev: EventStreamElement[LagompbEvent]): KafkaEvent = {
    ev.event match {
      case EventWrapper(Some(anyEvent: Any), Some(state: Any), Some(metaData: MetaData)) =>
        Try {
          LagompbProtosRegistry
            .getCompanion(anyEvent)
            .flatMap(comp => {
              comp.scalaDescriptor.fields
                .find(
                  field =>
                    field.getOptions
                      .extension(ExtensionsProto.kafka)
                      .exists(_.partitionKey)
                )
                .map(fd => {
                  KafkaEvent()
                    .withEvent(anyEvent)
                    .withState(StateWrapper().withMeta(metaData).withState(state))
                    .withPartitionKey(
                      comp
                        .parseFrom(anyEvent.value.toByteArray)
                        .getField(fd)
                        .as[String]
                    )
                    .withServiceName(serviceName)
                })
            })
        } match {
          case Failure(exception) =>
            throw new LagompbException(s"companion not found for ${anyEvent.typeUrl}. reason: ${exception.getMessage}")
          case Success(result: Option[KafkaEvent]) =>
            result match {
              case Some(value: KafkaEvent) =>
                value
              case None =>
                throw new LagompbException(s"unable to transform event ${anyEvent.typeUrl} for kafka")
            }
        }
      case _ =>
        throw new LagompbException(s"unknown event received ${ev.event.getClass.getName}")
    }
  }

}

/**
 * LagompbServiceImpl abstract class.
 *
 * It must be implemented by any lagom REST based service
 *
 * @param clusterSharding          the cluster sharding
 * @param persistentEntityRegistry the persistence entity registry
 * @param ec                       the execution context
 */
abstract class LagompbServiceImpl(
    val clusterSharding: ClusterSharding,
    val persistentEntityRegistry: PersistentEntityRegistry,
    val aggregate: LagompbAggregate[_]
)(implicit ec: ExecutionContext, timeout: Timeout)
    extends LagompbRestServiceImpl(clusterSharding, persistentEntityRegistry)(ec, timeout)
    with LagompbService {

  final override def aggregateRoot: LagompbAggregate[_] = aggregate
}

/**
 * LagompbGrpcServiceImpl
 */
trait LagompbGrpcServiceImpl extends LagompbBaseServiceImpl {

  /**
   * Sends command to the aggregate root. The command must have the aggregate entity id set.
   * When the entity id is not set a INVALID_ARGUMENT is sent to the gRPC client
   *
   * @param clusterSharding the cluster sharding
   * @param entityId        the entity ID
   * @param cmd             the command to send
   * @param data            the additional data to send
   * @tparam TCommand the command scala type
   * @tparam TState   the actual state scala type
   * @return the [[lagompb.LagompbState]] containing the actual state and the state meta
   */
  final override def sendCommand[TCommand <: GeneratedMessage, TState <: scalapb.GeneratedMessage](
      clusterSharding: ClusterSharding,
      entityId: String,
      cmd: TCommand,
      data: Map[String, String]
  )(implicit ec: ExecutionContext, timeout: Timeout): Future[LagompbState[TState]] = {
    super
      .sendCommand[TCommand, TState](clusterSharding, entityId, cmd, data)
      .transform {
        case Failure(exception) =>
          exception match {
            case e: LagompbException =>
              Failure(new GrpcServiceException(status = Status.INTERNAL.withDescription(e.getMessage)))
            case e: GrpcServiceException => Failure(e)
            case e: LagompbInvalidCommandException =>
              Failure(new GrpcServiceException(status = Status.INVALID_ARGUMENT.withDescription(e.getMessage)))
            case _ =>
              Failure(new GrpcServiceException(status = Status.INTERNAL))
          }
        case Success(value) => Success(value)
      }
  }

  /**
   * Sends command to the aggregate root. The command must have the aggregate entity id set.
   * When the entity id is not set a INVALID_ARGUMENT is sent to the gRPC client
   *
   * @param clusterSharding the cluster sharding
   * @param cmd             the command to send
   * @param data            the additional data to send
   * @tparam TCommand the command scala type
   * @tparam TState   the actual state scala type
   * @return the [[lagompb.LagompbState]] containing the actual state and the state meta
   */
  final def sendCommand[TCommand <: GeneratedMessage, TState <: scalapb.GeneratedMessage](
      clusterSharding: ClusterSharding,
      cmd: TCommand,
      data: Map[String, String]
  )(implicit ec: ExecutionContext, timeout: Timeout): Future[LagompbState[TState]] = {

    cmd.companion.scalaDescriptor.fields
      .find(field => field.getOptions.extension(ExtensionsProto.command).exists(_.entityId))
      .fold[Future[LagompbState[TState]]](
        Future.failed(
          new GrpcServiceException(
            status = Status.INVALID_ARGUMENT
              .withDescription("command does not have entity key set.")
          )
        )
      )(fd => {
        val entityId: String = cmd.getField(fd).as[String]
        super
          .sendCommand[TCommand, TState](clusterSharding, entityId, cmd, data)
          .transform {
            case Failure(exception) =>
              exception match {
                case e: LagompbException =>
                  Failure(new GrpcServiceException(status = Status.INTERNAL.withDescription(e.getMessage)))
                case e: GrpcServiceException => Failure(e)
                case e: LagompbInvalidCommandException =>
                  Failure(new GrpcServiceException(status = Status.INVALID_ARGUMENT.withDescription(e.getMessage)))
                case _ =>
                  Failure(new GrpcServiceException(status = Status.INTERNAL))
              }
            case Success(value) => Success(value)
          }
      })
  }
}
