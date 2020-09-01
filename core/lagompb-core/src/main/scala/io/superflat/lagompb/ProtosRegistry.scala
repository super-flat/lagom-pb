package io.superflat.lagompb

import com.google.protobuf.any.Any
import org.reflections.Reflections
import org.slf4j.{Logger, LoggerFactory}
import scalapb.{GeneratedFileObject, GeneratedMessage, GeneratedMessageCompanion}
import scalapb.json4s.{Parser, Printer, TypeRegistry}

import scala.jdk.CollectionConverters._
import scala.reflect.runtime.universe
import scala.util.{Failure, Success, Try}
import scala.collection.mutable

/**
 * Helpful registry of scalapb protobuf classes that can be used to find
 * companion objects, do JSON serialization, and unpack "Any" messages
 *
 * @param registry a sequence of GeneratedFileObjects (likely from reflection)
 */
class ProtosRegistry(private[lagompb] val registry: Seq[GeneratedFileObject]) {

  /**
   * scalapb generated message companions list
   */
  private[lagompb] val companions: Vector[GeneratedMessageCompanion[_ <: GeneratedMessage]] =
    registry
      .foldLeft[Vector[scalapb.GeneratedMessageCompanion[_ <: scalapb.GeneratedMessage]]](Vector.empty) {
        (s, fileObject) =>
          s ++ fileObject.messagesCompanions
      }

  /**
   * Creates a map between the generated message typeUrl and the appropriate message companion
   */
  private[lagompb] val companionsMap: Map[String, scalapb.GeneratedMessageCompanion[_ <: scalapb.GeneratedMessage]] =
    companions
      .map(companion => (companion.scalaDescriptor.fullName, companion))
      .toMap

  private[lagompb] val typeRegistry: TypeRegistry =
    registry
      .foldLeft(TypeRegistry.empty) { (reg: TypeRegistry, fileObject) =>
        reg.addFile(fileObject)
      }

  private[lagompb] val parser: Parser =
    new Parser().withTypeRegistry(typeRegistry)

  private[lagompb] val printer: Printer =
    new Printer().includingDefaultValueFields
      .withTypeRegistry(typeRegistry)

  /**
   * Gets the maybe scalapb GeneratedMessageCompanion object defining an Any protobuf message
   * @param any the protobuf message
   * @return the maybe scalapb GeneratedMessageCompanion object
   */
  def getCompanion(any: Any): Option[GeneratedMessageCompanion[_ <: GeneratedMessage]] =
    any.typeUrl.split('/').lastOption.flatMap(companionsMap.get)

  /**
   * Unpack a proto Any into its scalapb class, or fail
   *
   * @param any scalapb google Any protobuf
   * @return Successful unpacked message or a Failure
   */
  def unpackAny(any: Any): Try[_ <: GeneratedMessage] = {
    getCompanion(any) match {
      case None       => Failure(new Exception(s"could not unpack unrecognized proto ${any.typeUrl}"))
      case Some(comp) => Try(any.unpack(comp))
    }
  }

  /**
   * unpack many Any messages or exit on first failure
   *
   * @param anys many any messages
   * @return array with unpacked messages in the same order as the input anys
   */
  def unpackAnys(anys: Any*): Try[Seq[GeneratedMessage]] = {

    val folder = (tryBuffer: Try[mutable.ListBuffer[GeneratedMessage]], any: Any) => {
      tryBuffer.flatMap(buffer => unpackAny(any).map(msg => buffer.append(msg)))
    }

    val buffer: mutable.ListBuffer[GeneratedMessage] = mutable.ListBuffer[GeneratedMessage]()

    anys.foldLeft(Try(buffer))(folder).map(_.toSeq)
  }
}

/**
 * Companion object for ProtosRegistry
 */
object ProtosRegistry {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  /**
   * Load scalapb generated  fileobjects that contain proto companions messages
   * @return the sequence of scalapb.GeneratedFileObject
   */
  @throws(classOf[ScalaReflectionException])
  private def load(): Seq[GeneratedFileObject] = {
    val fileObjects: Seq[Class[_ <: GeneratedFileObject]] =
      new Reflections(ConfigReader.protosPackage)
        .getSubTypesOf(classOf[scalapb.GeneratedFileObject])
        .asScala
        .toSeq

    fileObjects.foldLeft(Seq.empty[GeneratedFileObject]) { (seq, fo) =>
      Try {
        val runtimeMirror: universe.Mirror =
          universe.runtimeMirror(fo.getClassLoader)
        val module: universe.ModuleSymbol =
          runtimeMirror.staticModule(fo.getName)
        runtimeMirror
          .reflectModule(module)
          .instance
          .asInstanceOf[GeneratedFileObject]
      } match {
        case Failure(exception) =>
          exception match {
            case e: ScalaReflectionException => throw e
            case _                           => seq
          }
        case Success(fileObject) =>
          val subMsg: String = fileObject.messagesCompanions
            .map(mc =>
              s"\n|\t\t - companion typeUrl: ${mc.scalaDescriptor.fullName}, jvmName: ${mc.getClass.getCanonicalName}"
            )
            .mkString("")
          val msg: String =
            s"|\t - fileObject jvmName: ${fileObject.getClass.getCanonicalName}"
          logger.debug(s"$msg$subMsg")
          seq :+ fileObject
      }
    }
  }

  /**
   * instantiate a ProtosRegistry by reflecting all scalapb objects
   *
   * @return instantiated ProtosRegistry
   */
  def fromReflection(): ProtosRegistry = {
    val registry = load()
    new ProtosRegistry(registry)
  }
}
