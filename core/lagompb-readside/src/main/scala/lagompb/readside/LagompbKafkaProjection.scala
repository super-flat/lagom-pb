package lagompb.readside

import akka.Done
import akka.actor.{ActorSystem => ActorSystemClassic}
import akka.kafka.scaladsl.SendProducer
import akka.kafka.{ProducerMessage, ProducerSettings}
import akka.projection.eventsourced.EventEnvelope
import com.google.protobuf.any
import com.typesafe.config.Config
import lagompb.protobuf.core.{EventWrapper, StateWrapper}
import lagompb.protobuf.extensions.ExtensionsProto
import lagompb.readside.utils.LagompbProducerConfig
import lagompb.util.LagompbProtosCompanions
import lagompb.{LagompbEvent, LagompbException}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import scalapb.descriptors.FieldDescriptor
import slick.dbio.{DBIO, DBIOAction}

import scala.concurrent.ExecutionContext

/**
 * Helps push snapshots and journal events to kafka
 *
 * @param config
 * @param actorSystem
 * @param ec
 * @tparam TState
 */
abstract class LagompbKafkaProjection[TState <: scalapb.GeneratedMessage](
    config: Config,
    actorSystem: ActorSystemClassic
)(implicit ec: ExecutionContext)
    extends LagompbProjection[TState](config, actorSystem) {

  // The implementation class needs to set the akka.kafka.producer settings in the config file as well
  // as the lagompb.kafka-projections
  val producerConfig: LagompbProducerConfig = LagompbProducerConfig(
    actorSystem.settings.config.getConfig(" lagompb.projection.kafka")
  )

  private val sendProducer = SendProducer(
    ProducerSettings(actorSystem, new StringSerializer, new ByteArraySerializer)
      .withBootstrapServers(producerConfig.bootstrapServers)
  )(actorSystem)

  override def process(envelope: EventEnvelope[LagompbEvent]): DBIO[Done] = {
    envelope.event match {
      case EventWrapper(Some(event: any.Any), Some(resultingState), Some(meta)) =>
        LagompbProtosCompanions
          .getCompanion(event) match {
          case Some(comp) =>
            comp.scalaDescriptor.fields
              .find(
                field =>
                  field.getOptions
                    .extension(ExtensionsProto.kafka)
                    .exists(_.partitionKey)
              ) match {
              case Some(fd: FieldDescriptor) =>
                // get the partition key
                val partitionKey = comp
                  .parseFrom(event.value.toByteArray)
                  .getField(fd)
                  .as[String]

                // let us wrap the state and the meta data and persist to kafka
                val stateWrapper = StateWrapper().withMeta(meta).withState(resultingState)

                val kafkaMessages: ProducerMessage.Envelope[String, Array[Byte], String] =
                  ProducerMessage.multi(
                    Seq(
                      new ProducerRecord(producerConfig.stateTopic, partitionKey, stateWrapper.toByteArray),
                      new ProducerRecord(producerConfig.eventsTopic, partitionKey, event.value.toByteArray)
                    ),
                    ""
                  )

                // let us publish the messages to kafka
                val result = sendProducer.sendEnvelope(kafkaMessages).map { _ =>
                  Done
                }

                DBIO.from(result)

              case None =>
                DBIOAction.failed(new LagompbException(s"No partition key field is defined for event ${event.typeUrl}"))
            }

          case None => DBIOAction.failed(new LagompbException(s"companion not found for ${event.typeUrl}"))
        }
      case _ =>
        DBIO.failed(new LagompbException(s"[Lagompb] unknown event received ${envelope.event.getClass.getName}"))
    }
  }
}
