package io.superflat.lagompb.readside

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.SendProducer
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.projection.{ProjectionBehavior, ProjectionId}
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.slick.SlickProjection
import com.google.protobuf.any
import io.superflat.lagompb.{ConfigReader, GlobalException, ProtosRegistry}
import io.superflat.lagompb.encryption.ProtoEncryption
import io.superflat.lagompb.protobuf.core.{KafkaEvent, MetaData, StateWrapper}
import io.superflat.lagompb.protobuf.encryption.EncryptedProto
import io.superflat.lagompb.protobuf.extensions.ExtensionsProto
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.{Logger, LoggerFactory}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}
import scalapb.descriptors.FieldDescriptor
import slick.basic.DatabaseConfig
import slick.dbio.{DBIO, DBIOAction}
import slick.jdbc.PostgresProfile

import scala.concurrent.ExecutionContext

/**
 * Helps handle readSide processor by pushing persisted events to kafka and storing the offsets
 * in postgres.
 *
 * @param encryption ProtoEncryption instance to use
 * @param actorSystem the actor system
 * @param ec the execution context
 * @tparam T the aggregate state type
 */

abstract class KafkaPublisher[T <: scalapb.GeneratedMessage](encryption: ProtoEncryption)(implicit
    ec: ExecutionContext,
    actorSystem: ActorSystem[_]
) extends EventProcessor {

  final val log: Logger = LoggerFactory.getLogger(getClass)
  // The implementation class needs to set the akka.kafka.producer settings in the config file as well
  // as the lagompb.kafka-projections
  val producerConfig: KafkaConfig = KafkaConfig(actorSystem.settings.config.getConfig(" lagompb.projection.kafka"))

  // The implementation class needs to set the akka.projection.slick config for the offset database
  protected val dbConfig: DatabaseConfig[PostgresProfile] =
    DatabaseConfig.forConfig("akka.projection.slick", actorSystem.settings.config)
  protected val baseTag: String = ConfigReader.eventsConfig.tagName

  private[this] val sendProducer: SendProducer[String, String] = SendProducer(
    ProducerSettings(actorSystem, new StringSerializer, new StringSerializer)
      .withBootstrapServers(producerConfig.bootstrapServers)
  )(actorSystem.toClassic)

  final override def process(
      comp: GeneratedMessageCompanion[_ <: GeneratedMessage],
      event: any.Any,
      eventTag: String,
      resultingState: any.Any,
      meta: MetaData
  ): DBIO[Done] =
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
              ProtosRegistry.printer.print(
                KafkaEvent.defaultInstance
                  .withEvent(event)
                  .withState(StateWrapper().withMeta(meta).withState(resultingState))
                  .withPartitionKey(
                    comp
                      .parseFrom(event.value.toByteArray)
                      .getField(fd)
                      .as[String]
                  )
                  .withServiceName(ConfigReader.serviceName)
              )
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
        DBIOAction.failed(new GlobalException(s"No partition key field is defined for event ${event.typeUrl}"))
    }

  /**
   * Initialize the projection to start fetching the events that are emitted
   */
  def init(): Unit =
    ShardedDaemonProcess(actorSystem).init[ProjectionBehavior.Command](
      name = projectionName,
      numberOfInstances = ConfigReader.allEventTags.size,
      behaviorFactory = (n: Int) => {
        val tagName: String = s"$baseTag$n"
        ProjectionBehavior(
          SlickProjection
            .exactlyOnce(
              projectionId = ProjectionId(projectionName, tagName),
              EventSourcedProvider
                .eventsByTag[EncryptedProto](actorSystem, readJournalPluginId = JdbcReadJournal.Identifier, tagName),
              dbConfig,
              handler = () => new EventsReader(tagName, encryption, this)
            )
        )
      },
      settings = ShardedDaemonProcessSettings(actorSystem),
      stopMessage = Some(ProjectionBehavior.Stop)
    )

  /**
   * The projection Name must be unique
   *
   * @return
   */
  def projectionName: String

  /**
   * aggregate state. it is a generated scalapb message extending the LagompbState trait
   *
   * @return aggregate state
   */
  def aggregateStateCompanion: scalapb.GeneratedMessageCompanion[T]
}
