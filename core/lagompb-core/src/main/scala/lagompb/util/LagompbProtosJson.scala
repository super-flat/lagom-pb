package lagompb.util

import play.api.libs.json._
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}
import scalapb_json.TypeRegistry
import scalapb_playjson.{Parser, Printer}

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

/**
 * Helps provide implicit serialization utilities for api requests and responses
 * using protocol buffer messages
 */
trait LagompbProtosJson {

  implicit def writes[A <: GeneratedMessage: GeneratedMessageCompanion]: Writes[A] =
    (o: A) => printer.toJson(o)

  implicit def reads[A <: GeneratedMessage: GeneratedMessageCompanion]: Reads[A] =
    (json: JsValue) =>
      Try[A](parser.fromJson[A](json)) match {
        case Success(value) => JsSuccess(value)
        case Failure(f) => JsError(f.getMessage)
    }

  private lazy val typeRegistry: TypeRegistry =
    LagompbCommon
      .loadFileObjects()
      .foldLeft(TypeRegistry.empty)({ (reg, fileObject) =>
        reg.addFile(fileObject)
      })

  private lazy val parser: Parser =
    new Parser().withTypeRegistry(typeRegistry)
  private lazy val printer: Printer =
    new Printer().includingDefaultValueFields
      .withTypeRegistry(typeRegistry)
}
