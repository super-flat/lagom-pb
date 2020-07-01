package io.superflat.lagompb.encryption

import akka.persistence.typed.{EventAdapter, EventSeq, SnapshotAdapter}
import com.google.protobuf.any.Any
import scala.util.Try

/**
 * Type safe base class for persistence snapshot adapter
 */
abstract class TypesafeSnapshotAdapter[T, U] extends SnapshotAdapter[T] {

  /**
   * type-safe method for converting state of type `T` to persisted type `U`
   *
   * @param state instance of `T`
   * @return instance of U
   */
  def safeToJournal(state: T): U

  /**
   * generic method for type-safe conversion from type `U` to `T` for
   * reading persisted snapshots.
   *
   * @param from
   * @return
   */
  def safeFromJournal(from: U): T

  /**
   * implements the SnapshotAdapter method using provided `safeToJournal`
   * method
   *
   * @param state state of type `T`
   * @return any scala object
   */
  final def toJournal(state: T): scala.Any = {
    safeToJournal(state)
  }

  /**
   * implements SnapshotAdapter method using provided `safeFromJournal` method
   *
   * @param from any scala object
   * @return instance of `T`
   */
  final def fromJournal(from: scala.Any): T = {
    safeFromJournal(from.asInstanceOf[U])
  }
}
