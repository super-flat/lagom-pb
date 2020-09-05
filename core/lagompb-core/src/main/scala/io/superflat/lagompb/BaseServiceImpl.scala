package io.superflat.lagompb

import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.grpc.GrpcServiceException
import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import io.grpc.Status
import io.superflat.lagompb.protobuf.v1.core.{FailedReply, FailureCause, StateWrapper}
import org.slf4j.{Logger, LoggerFactory}
import scalapb.GeneratedMessage

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

trait SharedBaseServiceImpl extends SendCommand {
  // some cluster sharding instance
  def clusterSharding: ClusterSharding

  // aggregateRoot defines the persistent entity that will be used to handle commands
  def aggregateRoot: AggregateRoot[_]

  /**
   * wrapper method to invoke sendCommand and pass in cluster sharding
   *
   * @param entityId         the entity Id added or retrieved from the shard
   * @param cmd              the command to send to the aggregate. It is a scalapb generated case class from the command
   *                         protocol buffer message definition
   * @param data             additional data that need to be set in the state meta
   * @return Future of state
   */
  def sendCommand(entityId: String, cmd: GeneratedMessage, data: Map[String, String])(implicit
    ec: ExecutionContext
  ): Future[StateWrapper] = sendCommand(clusterSharding, aggregateRoot, entityId, cmd, data)(ec)

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
  val aggregate: AggregateRoot[_]
) extends BaseService
    with SharedBaseServiceImpl {

  final val log: Logger = LoggerFactory.getLogger(getClass)

  def aggregateRoot: AggregateRoot[_] = aggregate

  /**
   * generic conversion for failed replys into a scala Failure
   * with a gRPC exception
   *
   * @param failedReply some command handler failed reply
   * @return a Failure of type Try[StateWrapper]
   */
  override def transformFailedReply(failedReply: FailedReply): Failure[Throwable] =
    failedReply.cause match {

      case FailureCause.VALIDATION_ERROR =>
        Failure(BadRequest(failedReply.reason))

      case _ if failedReply.reason.nonEmpty =>
        Failure(InternalServerError(failedReply.reason))

      case _ =>
        Failure(InternalServerError("critical failure"))
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
   * @param failedReply some command handler failed reply
   * @return a Failure of type Try[StateWrapper]
   */
  override def transformFailedReply(failedReply: FailedReply): Failure[Throwable] = {
    val status: Status = failedReply.cause match {
      case FailureCause.VALIDATION_ERROR =>
        Status.INVALID_ARGUMENT.withDescription(failedReply.reason)

      case FailureCause.INTERNAL_ERROR =>
        Status.INTERNAL.withDescription(failedReply.reason)

      case _ =>
        Status.INTERNAL.withDescription(failedReply.reason)
    }

    Failure(new GrpcServiceException(status = status))
  }
}
