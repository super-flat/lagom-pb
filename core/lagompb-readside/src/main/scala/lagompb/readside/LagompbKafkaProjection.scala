package lagompb.readside

import akka.Done
import akka.actor.{ActorSystem => ActorSystemClassic}
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.SendProducer
import akka.projection.eventsourced.EventEnvelope
import com.google.protobuf.any
import com.typesafe.config.Config
import lagompb.protobuf.core.{EventWrapper, KafkaEvent, MetaData, StateWrapper}
import lagompb.protobuf.extensions.ExtensionsProto
import lagompb.util.LagompbProtosCompanions
import lagompb.{LagompbEvent, LagompbException}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import scalapb.descriptors.FieldDescriptor
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}
import slick.dbio.{DBIO, DBIOAction}

import scala.concurrent.ExecutionContext

/**
 * Helps push snapshots and journal events to kafka
 *
 * @param config the configuration instance
 * @param actorSystem the actor system
 * @param ec the execution context
 * @tparam TState the aggregate state type
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

  final override def handleEvent(
      comp: GeneratedMessageCompanion[_ <: GeneratedMessage],
      event: any.Any,
      resultingState: any.Any,
      meta: MetaData
  ): DBIO[Done] = {
    comp.scalaDescriptor.fields
      .find(
        field =>
          field.getOptions
            .extension(ExtensionsProto.kafka)
            .exists(_.partitionKey)
      ) match {
      case Some(fd: FieldDescriptor) =>
        // let us wrap the state and the meta data and persist to kafka
        val result = sendProducer
          .send(
            new ProducerRecord(
              producerConfig.topic,
              comp
                .parseFrom(event.value.toByteArray)
                .getField(fd)
                .as[String],
              KafkaEvent.defaultInstance
                .withEvent(event)
                .withState(StateWrapper().withMeta(meta).withState(resultingState))
                .withPartitionKey(
                  comp
                    .parseFrom(event.value.toByteArray)
                    .getField(fd)
                    .as[String]
                )
                .withServiceName(config.getString("lagompb.service-name"))
                .toByteArray
            )
          )
          .map { recordMetadata =>
            log.info(
              "Published event [{}] and state [{}] to topic/partition {}/{}",
              event.typeUrl,
              resultingState.typeUrl,
              producerConfig.topic,
              recordMetadata.partition
            )
            Done
          }

        DBIO.from(result)

      case None =>
        DBIOAction.failed(new LagompbException(s"No partition key field is defined for event ${event.typeUrl}"))
    }
  }
}
