package io.superflat.lagompb

import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.grpc.GrpcServiceException
import akka.util.Timeout
import cats.implicits._
import com.google.protobuf.any.Any
import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import io.grpc.Status
import io.superflat.lagompb.v1.protobuf.core._
import io.superflat.lagompb.v1.protobuf.core.CommandReply.Reply
import io.superflat.lagompb.v1.protobuf.extensions.ExtensionsProto
import org.slf4j.{Logger, LoggerFactory}
import scalapb.GeneratedMessage

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

sealed trait SharedBaseServiceImpl {
  // $COVERAGE-OFF$
  implicit val timeout: Timeout = ConfigReader.askTimeout

  /**
   * aggregateRoot defines the persistent entity that will be used to handle commands
   *
   * @see [[io.superflat.lagompb.AggregateRoot]].
   *      Also for more info refer to the lagom doc [[https://www.lagomframework.com/documentation/1.6.x/scala/UsingAkkaPersistenceTyped.html]]
   */
  def aggregateRoot: AggregateRoot[_]

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
   * @param entityId the entity Id added or retrieved from the shard
   * @param cmd        the command to send to the aggregate. It is a scalapb generated case class from the command
   *                   protocol buffer message definition
   * @param data       additional data that need to be set in the state meta
   * @tparam C the Type of the command to send.
   * @return Future of state
   */
  def sendCommand[C <: scalapb.GeneratedMessage, S <: scalapb.GeneratedMessage](
    clusterSharding: ClusterSharding,
    entityId: String,
    cmd: C,
    data: Map[String, String]
  )(implicit ec: ExecutionContext): Future[StateAndMeta[S]] =
    clusterSharding
      .entityRefFor(aggregateRoot.typeKey, entityId)
      .ask[CommandReply](replyTo => Command(cmd, replyTo, data))
      .map((value: CommandReply) => handleLagompbCommandReply[S](value))

  private[lagompb] def handleLagompbCommandReply[S <: scalapb.GeneratedMessage](
    commandReply: CommandReply
  ): StateAndMeta[S] =
    commandReply.reply match {
      case Reply.SuccessfulReply(successReply) =>
        parseState[S](successReply.getStateWrapper)
      case Reply.FailedReply(failureReply) =>
        failureReply.cause match {
          case FailureCause.VALIDATION_ERROR =>
            throw new InvalidCommandException(failureReply.reason)
          case FailureCause.INTERNAL_ERROR =>
            throw new GlobalException(failureReply.reason)
          case _ => throw new GlobalException("reason unknown")
        }
      case _ =>
        throw new GlobalException(s"unknown CommandReply ${commandReply.reply.getClass.getName}")
    }

  private[lagompb] def parseState[S <: scalapb.GeneratedMessage](
    stateWrapper: StateWrapper
  ): StateAndMeta[S] = {
    val meta: MetaData = stateWrapper.getMeta
    val state: Any = stateWrapper.getState
    val parsed: S = parseAny[S](state)
    StateAndMeta[S](parsed, meta)
  }

  private[lagompb] def parseAny[S <: scalapb.GeneratedMessage](data: Any): S = {
    val typeUrl: String = data.typeUrl.split('/').lastOption.getOrElse("")

    if (aggregateStateCompanion.scalaDescriptor.fullName === typeUrl)
      Try {
        data.unpack(aggregateStateCompanion).asInstanceOf[S]
      } match {
        case Failure(exception) =>
          throw new GlobalException(exception.getMessage)
        case Success(value) => value
      }
    else throw new GlobalException("wrong state definition")
  }

  // $COVERAGE-ON$
}

/**
 * BaseServiceImpl abstract class.
 *
 * It must be implemented by any lagom REST based service
 *
 * @param clusterSharding          the cluster sharding
 * @param persistentEntityRegistry the persistence entity registry
 * @param ec                       the execution context
 */
abstract class BaseServiceImpl(
  val clusterSharding: ClusterSharding,
  val persistentEntityRegistry: PersistentEntityRegistry,
  val aggregate: AggregateRoot[_]
)(implicit ec: ExecutionContext)
    extends SharedBaseServiceImpl
    with BaseService {

  final val log: Logger = LoggerFactory.getLogger(getClass)

  final override def aggregateRoot: AggregateRoot[_] = aggregate

  /**
   * Sends command to the aggregate root. The command must have the aggregate entity id set.
   * When the entity id is not set a BadRequest is sent to the api
   *
   * @param cmd  the command to send
   * @param data the additional data to send
   * @tparam C the command scala type
   * @tparam S   the actual state scala type
   * @return the [[io.superflat.lagompb.StateAndMeta]] containing the actual state and the event meta
   */
  final def sendCommand[C <: GeneratedMessage, S <: scalapb.GeneratedMessage](
    cmd: C,
    data: Map[String, String] = Map.empty
  ): Future[StateAndMeta[S]] =
    cmd.companion.scalaDescriptor.fields
      .find(field => field.getOptions.extension(ExtensionsProto.command).exists(_.entityId))
      .fold[Future[StateAndMeta[S]]](Future.failed(BadRequest("entity key not set."))) { fd =>
        val entityId: String = cmd.getField(fd).as[String]
        super
          .sendCommand[C, S](clusterSharding, entityId, cmd, data)
          .transform {
            case Failure(exception) =>
              log.error("", exception)
              exception match {
                case e: GlobalException =>
                  Failure(InternalServerError(e.getMessage))
                case e: InvalidCommandException =>
                  Failure(BadRequest(e.getMessage))
                case _ => Failure(InternalServerError(""))
              }
            case Success(value) => Success(value)
          }
      }

  /**
   * Sends command to the aggregate root. The command must have the aggregate entity id set.
   * When the entity id is not set a BadRequest is sent to the api
   *
   * @param entityId the entity ID
   * @param cmd      the command to send
   * @param data     the additional data to send
   * @tparam C the command scala type
   * @tparam S   the actual state scala type
   * @return the [[io.superflat.lagompb.StateAndMeta]] containing the actual state and the event meta
   */
  final def sendCommand[C <: GeneratedMessage, S <: scalapb.GeneratedMessage](
    entityId: String,
    cmd: C,
    data: Map[String, String]
  ): Future[StateAndMeta[S]] =
    super
      .sendCommand[C, S](clusterSharding, entityId, cmd, data)
      .transform {
        case Failure(exception) =>
          log.error("", exception)
          exception match {
            case e: GlobalException =>
              Failure(InternalServerError(e.getMessage))
            case e: InvalidCommandException =>
              Failure(BadRequest(e.getMessage))
            case _ => Failure(InternalServerError(""))
          }
        case Success(value) => Success(value)
      }
}

/**
 * BaseGrpcServiceImpl
 */
trait BaseGrpcServiceImpl extends SharedBaseServiceImpl {

  // $COVERAGE-OFF$

  /**
   * Sends command to the aggregate root. The command must have the aggregate entity id set.
   * When the entity id is not set a INVALID_ARGUMENT is sent to the gRPC client
   *
   * @param clusterSharding the cluster sharding
   * @param entityId        the entity ID
   * @param cmd             the command to send
   * @param data            the additional data to send
   * @tparam C the command scala type
   * @tparam S   the actual state scala type
   * @return the [[io.superflat.lagompb.StateAndMeta]] containing the actual state and the state meta
   */
  final override def sendCommand[C <: GeneratedMessage, S <: scalapb.GeneratedMessage](
    clusterSharding: ClusterSharding,
    entityId: String,
    cmd: C,
    data: Map[String, String]
  )(implicit ec: ExecutionContext): Future[StateAndMeta[S]] =
    super
      .sendCommand[C, S](clusterSharding, entityId, cmd, data)
      .transform {
        case Failure(exception) =>
          exception match {
            case e: GlobalException =>
              Failure(new GrpcServiceException(status = Status.INTERNAL.withDescription(e.getMessage)))
            case e: GrpcServiceException => Failure(e)
            case e: InvalidCommandException =>
              Failure(new GrpcServiceException(status = Status.INVALID_ARGUMENT.withDescription(e.getMessage)))
            case _ =>
              Failure(new GrpcServiceException(status = Status.INTERNAL))
          }
        case Success(value) => Success(value)
      }

  /**
   * Sends command to the aggregate root. The command must have the aggregate entity id set.
   * When the entity id is not set a INVALID_ARGUMENT is sent to the gRPC client
   *
   * @param clusterSharding the cluster sharding
   * @param cmd             the command to send
   * @param data            the additional data to send
   * @tparam C the command scala type
   * @tparam S   the actual state scala type
   * @return the [[io.superflat.lagompb.StateAndMeta]] containing the actual state and the state meta
   */
  final def sendCommand[C <: GeneratedMessage, S <: scalapb.GeneratedMessage](
    clusterSharding: ClusterSharding,
    cmd: C,
    data: Map[String, String]
  )(implicit ec: ExecutionContext): Future[StateAndMeta[S]] =
    cmd.companion.scalaDescriptor.fields
      .find(field => field.getOptions.extension(ExtensionsProto.command).exists(_.entityId))
      .fold[Future[StateAndMeta[S]]](
        Future.failed(
          new GrpcServiceException(
            status = Status.INVALID_ARGUMENT
              .withDescription("entity key not set.")
          )
        )
      ) { fd =>
        val entityId: String = cmd.getField(fd).as[String]
        super
          .sendCommand[C, S](clusterSharding, entityId, cmd, data)
          .transform {
            case Failure(exception) =>
              exception match {
                case e: GlobalException =>
                  Failure(new GrpcServiceException(status = Status.INTERNAL.withDescription(e.getMessage)))
                case e: GrpcServiceException => Failure(e)
                case e: InvalidCommandException =>
                  Failure(new GrpcServiceException(status = Status.INVALID_ARGUMENT.withDescription(e.getMessage)))
                case _ =>
                  Failure(new GrpcServiceException(status = Status.INTERNAL))
              }
            case Success(value) => Success(value)
          }
      }

  // $COVERAGE-ON$
}
