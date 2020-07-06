package io.superflat.lagompb.readside

import akka.Done
import akka.actor.typed.ActorSystem
import com.google.protobuf.any
import io.superflat.lagompb.encryption.ProtoEncryption
import io.superflat.lagompb.protobuf.core.MetaData
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}
import slick.dbio.{DBIO, DBIOAction}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/**
 * ReadSideProcessor helps implement multiple readSide processors where the offsets are
 * persisted postgres. One of the greatest advantage is one can process events emitted differently by
 * spawning different type of [[ReadSideProcessor]] to handle them.
 * Each instance must be registered in the [[io.superflat.lagompb.BaseApplication]] implementation via
 * the dependency injection and the init method called
 *
 * Please bear in mind that the akka.projection.slick is required to be set in the configuration file.
 *
 * @see https://doc.akka.io/docs/akka-projection/current/slick.html#configuration
 * @param encryption ProtoEncryption instance to use
 * @param actorSystem the actor system
 * @param ec          the execution context
 * @tparam TState the aggregate state type
 */
abstract class ReadSideProcessor[TState <: scalapb.GeneratedMessage](encryption: ProtoEncryption)(implicit
    ec: ExecutionContext,
    actorSystem: ActorSystem[_]
) extends PostgresOffsetHandler[TState](encryption) {

  final override def handleEvent(
      comp: GeneratedMessageCompanion[_ <: GeneratedMessage],
      event: any.Any,
      resultingState: any.Any,
      meta: MetaData
  ): DBIO[Done] = {
    Try {
      handle(
        event.unpack(comp),
        resultingState
          .unpack[TState](aggregateStateCompanion),
        meta
      )
    } match {
      case Failure(exception) =>
        DBIOAction.failed(exception)
      case Success(result: Any) =>
        result
    }
  }

  /**
   * Handles aggregate event persisted and made available for read model
   *
   * @param event the aggregate event
   * @param state the Lagompb state that wraps the actual state and some meta data
   */
  def handle(event: scalapb.GeneratedMessage, state: TState, metaData: MetaData): DBIO[Done]
}
