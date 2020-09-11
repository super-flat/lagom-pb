package io.superflat.lagompb

import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.grpc.GrpcServiceException
import com.lightbend.lagom.scaladsl.api.transport.{BadRequest, NotFound}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import io.grpc.Status
import io.superflat.lagompb.protobuf.v1.core.FailureResponse.FailureType
import io.superflat.lagompb.protobuf.v1.core.{FailureResponse, StateWrapper}
import org.slf4j.{Logger, LoggerFactory}
import scalapb.GeneratedMessage

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

trait SharedBaseServiceImpl extends SendCommand {
  // some cluster sharding instance
  def clusterSharding: ClusterSharding

  // aggregateRoot defines the persistent entity that will be used to handle commands
  def aggregateRoot: AggregateRoot

  /**
   * wrapper method to invoke sendCommand and pass in cluster sharding
   *
   * @param entityId         the entity Id added or retrieved from the shard
   * @param cmd              the command to send to the aggregate. It is a scalapb generated case class from the command
   *                         protocol buffer message definition
   * @param data             additional data that need to be set in the state meta
   * @return Future of state
   */
  def sendCommand(
      entityId: String,
      cmd: GeneratedMessage,
      data: Map[String, String]
  )(implicit
      ec: ExecutionContext
  ): Future[StateWrapper] = {
    sendCommand(clusterSharding, aggregateRoot, entityId, cmd, data)(ec)
  }
}

/**
 * BaseServiceImpl abstract class.
 *
 * It must be implemented by any lagom REST based service
 *
 * @param clusterSharding          the cluster sharding
 * @param persistentEntityRegistry the persistence entity registrythe execution context
 */
abstract class BaseServiceImpl(
    val clusterSharding: ClusterSharding,
    val persistentEntityRegistry: PersistentEntityRegistry,
    val aggregateRoot: AggregateRoot
) extends BaseService
    with SharedBaseServiceImpl {

  final val log: Logger = LoggerFactory.getLogger(getClass)

  /**
   * generic conversion for failed replys into a scala Failure
   * with a gRPC exception
   *
   * @param failureResponse some command handler failed reply
   * @return a Failure of type Try[StateWrapper]
   */
  override def transformFailedReply(
      failureResponse: FailureResponse
  ): Failure[Throwable] = {
    failureResponse.failureType match {
      case FailureType.Critical(value) => Failure(Http500(value))

      case FailureType.Custom(value) => Failure(Http500(s"unhandled custom error: ${value.typeUrl}"))

      case FailureType.Validation(value) =>
        Failure(BadRequest(value))

      case FailureType.NotFound(value) => Failure(NotFound(value))

      case FailureType.Empty =>
        Failure(Http500("unknown failure type"))

    }
  }
}

/**
 * BaseGrpcServiceImpl
 */
trait BaseGrpcServiceImpl extends SharedBaseServiceImpl {

  /**
   * generic conversion for failed replys into a scala Failure
   * with a gRPC exception
   *
   * @param failureResponse some command handler failed reply
   * @return a Failure of type Try[StateWrapper]
   */
  override def transformFailedReply(
      failureResponse: FailureResponse
  ): Failure[Throwable] = {

    failureResponse.failureType match {
      case FailureType.Critical(value) =>
        Failure(
          new GrpcServiceException(
            status = Status.INTERNAL.withDescription(value)
          )
        )

      case FailureType.Custom(value) =>
        Failure(
          new GrpcServiceException(
            status = Status.INTERNAL.withDescription(s"unhandled custom error: ${value.typeUrl}")
          )
        )

      case FailureType.Validation(value) =>
        Failure(
          new GrpcServiceException(
            status = Status.INVALID_ARGUMENT.withDescription(value)
          )
        )

      case FailureType.NotFound(value) =>
        Failure(
          new GrpcServiceException(
            status = Status.NOT_FOUND.withDescription(value)
          )
        )

      case FailureType.Empty =>
        Failure(
          new GrpcServiceException(
            status = Status.INTERNAL.withDescription("unknown failure type")
          )
        )

    }
  }
}
