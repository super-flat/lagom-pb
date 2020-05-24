package lagompb.util

import com.google.protobuf.any.Any
import scalapb.GeneratedMessage
import scalapb.GeneratedMessageCompanion

/**
 * Load scalapb proto generated message companions
 */
object LagompbProtosCompanions {

  /**
   * scalapb generated message companions list
   */
  lazy val companions: Vector[GeneratedMessageCompanion[_ <: GeneratedMessage]] = {
    LagompbCommon
      .loadFileObjects()
      .foldLeft[Vector[scalapb.GeneratedMessageCompanion[_ <: scalapb.GeneratedMessage]]](Vector.empty)({
        (s, fileObject) =>
          s ++ fileObject.messagesCompanions
      })
  }

  /**
   * Creates a map between the generated message typeUrl and the appropriate message companion
   */
  lazy val companionsMap: Map[String, scalapb.GeneratedMessageCompanion[_ <: scalapb.GeneratedMessage]] =
    companions
      .map(
        companion => (companion.scalaDescriptor.fullName, companion)
      )
      .toMap

  /**
   * Gets the maybe scalapb GeneratedMessageCompanion object defining an Any protobuf message
   * @param any the protobuf message
   * @return the maybe scalapb GeneratedMessageCompanion object
   */
  def getCompanion(any: Any): Option[GeneratedMessageCompanion[_ <: GeneratedMessage]] = {
    companionsMap.get(any.typeUrl.split('/').lastOption.getOrElse(""))
  }
}
