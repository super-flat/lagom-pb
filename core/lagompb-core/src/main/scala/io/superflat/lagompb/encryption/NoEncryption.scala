/*
 * Copyright (C) 2020 Superflat. <https://github.com/super-flat>
 */

package io.superflat.lagompb.encryption

import com.google.protobuf.ByteString
import com.google.protobuf.any.Any
import io.superflat.lagompb.protobuf.v1.encryption.EncryptedProto

import scala.util.Try

/**
 * Sample implementation of ProtoEncryption that packs the Any
 * message's bytestring into the EncryptedProto.
 */
object NoEncryption extends ProtoEncryption {

  /**
   * dummy encryptor that converts the given Any proto
   * into an EncryptedProto by converting to a bytestring
   *
   * @param proto input scalapb Any instance
   * @return Successful EncryptedProto instance or Failure
   */
  def encrypt(proto: Any): Try[EncryptedProto] = {
    // fake encryption, just make the default byte string
    val encryptedBytes: ByteString = proto.toByteString

    // pack byte string into the encrypted proto
    Try(EncryptedProto().withEncryptedProto(encryptedBytes))
  }

  /**
   * convert an EncryptedProto back to a scalapb Any instance
   * by unpacking the bytestring and casting as an Any
   *
   * @param encryptedProto instance of EncryptedProto
   * @return Successful scalapb Any instance or Failure
   */
  def decrypt(encryptedProto: EncryptedProto): Try[Any] =
    Try {
      // fake decryption back to ByteString -> ByteArray -> Any
      val encryptedByteString: ByteString = encryptedProto.encryptedProto
      val byteArray: Array[Byte] = encryptedByteString.toByteArray
      Any.parseFrom(byteArray)
    }
}
