package io.superflat.lagompb.encryption

import io.superflat.lagompb.protobuf.encryption.EncryptedProto

import scala.util.Try

/**
 * test encryption that flips booleans to show methods were run
 *
 * @param shouldFail true if encrypt/decrypt should throw
 */
class TestEncryption(shouldFail: Boolean = false) extends ProtoEncryption {

  var didEncrypt: Boolean = false
  var didDecrypt: Boolean = false

  val failureMsg: String = "test failure"

  def encrypt(proto: com.google.protobuf.any.Any): Try[EncryptedProto] = {
    didEncrypt = true
    if (shouldFail) throw new EncryptFailure(failureMsg)
    NoEncryption.encrypt(proto)
  }

  def decrypt(encryptedProto: EncryptedProto): Try[com.google.protobuf.any.Any] = {
    didDecrypt = true
    if (shouldFail) throw new DecryptPermanentFailure(failureMsg)
    NoEncryption.decrypt(encryptedProto)
  }

}
