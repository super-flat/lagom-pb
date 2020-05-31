package lagompb.util

import com.typesafe.config.{Config, ConfigFactory}
import org.reflections.Reflections
import org.slf4j.{Logger, LoggerFactory}
import scalapb.GeneratedFileObject

import scala.jdk.CollectionConverters._
import scala.reflect.runtime.universe
import scala.util.{Failure, Success, Try}

/**
 * Loads protocol buffer generated messages file object to build a registry of
 * scalapb message companions for the various serialization supported in lagompb
 */
object LagompbCommon {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  /**
   * Loads configuration into memory for usage
   */
  lazy val config: Config = ConfigFactory.load()

  private val packageName: String = config.getString("lagompb.protos-package")

  private lazy val reflections = new Reflections(packageName)

  /**
   * Load scalapb generated  fileobjects that contain proto companions messages
   * @return the sequence of scalapb.GeneratedFileObject
   */
  @throws(classOf[ScalaReflectionException])
  def loadFileObjects(): Seq[GeneratedFileObject] = {
    val fileObjects: Seq[Class[_ <: GeneratedFileObject]] = reflections
      .getSubTypesOf(classOf[scalapb.GeneratedFileObject])
      .asScala
      .toSeq

    fileObjects.foldLeft(Seq.empty[GeneratedFileObject])((seq, fo) => {
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
            case _ => seq
          }
        case Success(fileObject) =>
          val subMsg: String = fileObject.messagesCompanions
            .map(
              mc =>
                s"\n|\t\t - companion typeUrl: ${mc.scalaDescriptor.fullName}, jvmName: ${mc.getClass.getCanonicalName}"
            )
            .mkString("")
          val msg: String =
            s"|\t - fileObject jvmName: ${fileObject.getClass.getCanonicalName}"
          logger.debug(s"$msg$subMsg")
          seq :+ fileObject
      }
    })
  }
}
