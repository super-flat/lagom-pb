package io.superflat.lagompb.encryption

import com.google.protobuf.any.Any
import io.superflat.lagompb.v1.protobuf.encryption.EncryptedProto

import scala.util.Try

/**
 * signatures for encrypting protobuf, for use in the event and
 * snapshot adapters
 */
trait ProtoEncryption {

  /**
   * generic signature that accepts an Any protobuf and returns
   * a well-known EncryptedProto with nested encrypted bytes and
   * meta data used for decrypting later
   *
   * @param proto a scalapb Any instance
   * @return Successful EncryptedProto instance of Failure
   */
  def encrypt(proto: Any): Try[EncryptedProto]

  /**
   * generic signature for decrypting the well-known EncryptedProto
   *
   * @param encryptedProto instance of the EncryptedProto
   * @return Successful scalapb Any instance or Failure
   */
  def decrypt(encryptedProto: EncryptedProto): Try[Any]
}

/**
 * special error for failed encryption that can be returned
 * by implementers of the ProtoEncryption trait
 *
 * @param reason custom error message for the failure
 */
final case class EncryptFailure(reason: String) extends Throwable(reason)

/**
 * Custom failure for decryption that informs lagom-pb that the failure
 * is permanent and retires will always fail. This error was designed to be
 * thrown by ProtoEncryption implementations in situations like GDPR deletion,
 * where the key used to encrypt was revoked. Lagom-pb will reject commands
 * on the write side that throw this error, and it will skip/advance the
 * offset for events that cannot be decyprted on a read-side akka projection
 * implementation.
 *
 * @param reason custom error reason for this failure
 */
final case class DecryptPermanentFailure(reason: String) extends Throwable(reason)
