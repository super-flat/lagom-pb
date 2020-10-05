/*
 * Copyright (C) 2020 Superflat. <https://github.com/super-flat>
 */

package io.superflat.lagompb

import com.google.protobuf.any.Any
import org.reflections.Reflections
import org.slf4j.{Logger, LoggerFactory}
import scalapb.json4s.{Parser, Printer, TypeRegistry}
import scalapb.{GeneratedFileObject, GeneratedMessage, GeneratedMessageCompanion}

import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.reflect.runtime.universe
import scala.util.{Failure, Success, Try}

/**
 * Helpful registry of scalapb protobuf classes that can be used to find
 * companion objects, do JSON serialization, and unpack "Any" messages
 */
object ProtosRegistry {

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private[lagompb] lazy val registry: Seq[GeneratedFileObject] =
    reflectFileObjects()

  /**
   * scalapb generated message companions list
   */
  private[lagompb] lazy val companions: Vector[GeneratedMessageCompanion[_ <: GeneratedMessage]] =
    registry
      .foldLeft[Vector[
        scalapb.GeneratedMessageCompanion[_ <: scalapb.GeneratedMessage]
      ]](Vector.empty) { (s, fileObject) =>
        s ++ fileObject.messagesCompanions
      }

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
      .foldLeft(TypeRegistry.empty) { (reg: TypeRegistry, fileObject) =>
        reg.addFile(fileObject)
      }

  private[lagompb] lazy val parser: Parser =
    new Parser().withTypeRegistry(typeRegistry)

  private[lagompb] lazy val printer: Printer =
    new Printer().includingDefaultValueFields.formattingLongAsNumber
      .withTypeRegistry(typeRegistry)

  /**
   * Converts a scalapb GeneratedMessage to printable Json string using the available
   * registry
   * @param message the proto message
   * @return the json string
   */
  def toJson(message: GeneratedMessage): String = {
    printer.print(message)
  }

  /**
   * Converts a json string to a scalapb message
   *
   * @param jsonString the json string
   * @param A the scalapb message
   * @tparam A the scala type of the proto message to parse the json string into
   * @return the scalapb message parsed from the json string
   */
  def fromJson[A <: GeneratedMessage](
      jsonString: String
  )(implicit A: GeneratedMessageCompanion[A]): A = {
    parser.fromJsonString(jsonString)
  }

  /**
   * Gets the maybe scalapb GeneratedMessageCompanion object defining an Any protobuf message
   *
   * @param any the protobuf message
   * @return the maybe scalapb GeneratedMessageCompanion object
   */
  def getCompanion(
      any: Any
  ): Option[GeneratedMessageCompanion[_ <: GeneratedMessage]] =
    any.typeUrl.split('/').lastOption.flatMap(companionsMap.get)

  /**
   * Unpack a proto Any into its scalapb class, or fail
   *
   * @param any scalapb google Any protobuf
   * @return Successful unpacked message or a Failure
   */
  def unpackAny(any: Any): Try[GeneratedMessage] =
    getCompanion(any) match {
      case None =>
        Failure(
          new Exception(s"could not unpack unrecognized proto ${any.typeUrl}")
        )
      case Some(comp) => Try(any.unpack(comp))
    }

  /**
   * unpack many Any messages or exit on first failure
   *
   * @param anys many any messages
   * @return array with unpacked messages in the same order as the input anys
   */
  def unpackAnys(anys: Any*): Try[Seq[GeneratedMessage]] = {

    val folder =
      (tryBuffer: Try[mutable.ListBuffer[GeneratedMessage]], any: Any) => {
        tryBuffer.flatMap(buffer => unpackAny(any).map(msg => buffer.append(msg)))
      }

    val buffer: mutable.ListBuffer[GeneratedMessage] =
      mutable.ListBuffer[GeneratedMessage]()

    anys.foldLeft(Try(buffer))(folder).map(_.toSeq)
  }

  /**
   * Load scalapb generated  fileobjects that contain proto companions messages
   * @return the sequence of scalapb.GeneratedFileObject
   */
  @throws(classOf[ScalaReflectionException])
  private def reflectFileObjects(): Seq[GeneratedFileObject] = {
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
   * temporary helper method to force loading the lazy vals when needed
   */
  def load(): Unit = {
    registry
    companions
    companionsMap
    typeRegistry
    parser
    printer
  }
}
