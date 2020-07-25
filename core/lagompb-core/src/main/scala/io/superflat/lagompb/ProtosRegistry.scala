package io.superflat.lagompb

import com.google.protobuf.any.Any
import org.reflections.Reflections
import org.slf4j.{Logger, LoggerFactory}
import scalapb.{
  GeneratedFileObject,
  GeneratedMessage,
  GeneratedMessageCompanion
}
import scalapb.json4s.{Parser, Printer, TypeRegistry}

import scala.jdk.CollectionConverters._
import scala.reflect.runtime.universe
import scala.util.{Failure, Success, Try}

object ProtosRegistry {
  private[lagompb] lazy val registry: Seq[GeneratedFileObject] = load()

  /**
    * scalapb generated message companions list
    */
  private[lagompb] lazy val companions
      : Vector[GeneratedMessageCompanion[_ <: GeneratedMessage]] =
    registry
      .foldLeft[Vector[
        scalapb.GeneratedMessageCompanion[_ <: scalapb.GeneratedMessage]
      ]](Vector.empty)({ (s, fileObject) =>
        s ++ fileObject.messagesCompanions
      })

  /**
    * Creates a map between the generated message typeUrl and the appropriate message companion
    */
  private[lagompb] lazy val companionsMap: Map[
    String,
    scalapb.GeneratedMessageCompanion[_ <: scalapb.GeneratedMessage]
  ] =
    companions
      .map(companion => (companion.scalaDescriptor.fullName, companion))
      .toMap
  private[lagompb] lazy val typeRegistry: TypeRegistry =
    registry
      .foldLeft(TypeRegistry.empty)({ (reg, fileObject) =>
        reg.addFile(fileObject)
      })
  private[lagompb] lazy val parser: Parser =
    new Parser().withTypeRegistry(typeRegistry)
  private[lagompb] lazy val printer: Printer =
    new Printer().includingDefaultValueFields
      .withTypeRegistry(typeRegistry)
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  /**
    * Gets the maybe scalapb GeneratedMessageCompanion object defining an Any protobuf message
    * @param any the protobuf message
    * @return the maybe scalapb GeneratedMessageCompanion object
    */
  def getCompanion(
      any: Any
  ): Option[GeneratedMessageCompanion[_ <: GeneratedMessage]] =
    companionsMap.get(any.typeUrl.split('/').lastOption.getOrElse(""))

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
}
