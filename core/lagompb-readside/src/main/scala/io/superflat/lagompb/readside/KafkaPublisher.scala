package io.superflat.lagompb.readside

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.SendProducer
import com.google.protobuf.any.Any
import io.superflat.lagompb.encryption.EncryptionAdapter
import io.superflat.lagompb.protobuf.v1.core.{KafkaEvent, MetaData, StateWrapper}
import io.superflat.lagompb.protobuf.v1.extensions.ExtensionsProto
import io.superflat.lagompb.{ConfigReader, GlobalException, ProtosRegistry}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import scalapb.GeneratedMessage
import scalapb.descriptors.FieldDescriptor
import slick.dbio.{DBIO, DBIOAction}

import scala.concurrent.ExecutionContext

/**
 * ReadSideProcessor that publishes to kafka
 *
 * @param encryptionAdapter EncryptionAdapter instance to use
 * @param actorSystem the actor system
 * @param ec the execution context the aggregate state type
 */

abstract class KafkaPublisher(encryptionAdapter: EncryptionAdapter)(implicit
  ec: ExecutionContext,
  actorSystem: ActorSystem[_]
) extends TypedReadSideProcessor(encryptionAdapter) {

  // The implementation class needs to set the akka.kafka.producer settings in the config file as well
  // as the lagompb.kafka-projections
  val producerConfig: KafkaConfig = KafkaConfig(actorSystem.settings.config.getConfig("lagompb.projection.kafka"))

  private[this] val sendProducer: SendProducer[String, String] = SendProducer(
    ProducerSettings(actorSystem, new StringSerializer, new StringSerializer)
      .withBootstrapServers(producerConfig.bootstrapServers)
  )(actorSystem.toClassic)

  def handleTyped(
    event: GeneratedMessage,
    eventTag: String,
    resultingState: GeneratedMessage,
    meta: MetaData
  ): DBIO[Done] = {
    val anyEvent: Any = Any.pack(event)
    val anyState: Any = Any.pack(resultingState)

    event.companion.scalaDescriptor.fields.find(field =>
      // this should just be a function on the constructor
      // and it should be derived from State...
      field.getOptions.extension(ExtensionsProto.kafka).exists(_.partitionKey)
    ) match {
      case Some(fd: FieldDescriptor) =>
        // let us wrap the state and the meta data and persist to kafka
        DBIO.from(
          sendProducer
            .send(
              new ProducerRecord(
                producerConfig.topic,
                event.getField(fd).as[String],
                ProtosRegistry.printer.print(
                  KafkaEvent.defaultInstance
                    .withEvent(anyEvent)
                    .withState(StateWrapper().withMeta(meta).withState(anyState))
                    .withPartitionKey(event.getField(fd).as[String])
                    .withServiceName(ConfigReader.serviceName)
                )
              )
            )
            .map { recordMetadata =>
              log.info(
                "Published event [{}] and state [{}] to topic/partition {}/{}",
                anyEvent.typeUrl,
                anyState.typeUrl,
                producerConfig.topic,
                recordMetadata.partition
              )
              Done
            }
        )

      case None =>
        DBIOAction.failed(new GlobalException(s"No partition key field is defined for event ${anyEvent.typeUrl}"))
    }
  }
}
