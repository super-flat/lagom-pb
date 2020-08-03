package io.superflat.lagompb.encryption

import io.superflat.lagompb.protobuf.encryption.EncryptedProto
import com.google.protobuf.any.Any
import org.slf4j.{Logger, LoggerFactory}
import scala.util.{Success, Try}

/**
 * Adapter that optionally applies encryption if a ProtoEncryption is
 * provided, and otherwise passes through the provided Any message. This
 * allows users to implement a type-safe ProtoEncryption and not worry
 * about the ambiguous `Any` output.
 *
 * @param encryptor optional ProtoEncryption implementation
 */
class EncryptionAdapter(encryptor: Option[ProtoEncryption]) {

  import EncryptionAdapter.isEncryptedProto

  final val log: Logger = LoggerFactory.getLogger(getClass)

  /**
   * encrypt adapter that only applies encryption if ProtoEncryption is provided
   *
   * @param any a scalapb Any message to encrypt
   * @return Any message optionally encrypted with provided ProtoEncryption
   */
  def encrypt(any: Any): Try[Any] = {
    encryptor match {
      // if encryptor provided, attempt the encrypt
      case Some(enc) => enc.encrypt(any).map(encryptedProto => Any.pack(encryptedProto))
      // if no encryptor provided, pass through
      case None => Success(any)
    }
  }

  /**
   * decrypt adapter that only applies decryption if ProtoEncryption is provided
   *
   * @param any
   * @return
   */
  def decrypt(any: Any): Try[Any] = {
    encryptor match {
      // if an encryptor provided and it's an encrypted proto, attempt decrypt
      case Some(enc) if isEncryptedProto(any) =>
        Try(any.unpack(EncryptedProto)).flatMap(enc.decrypt)

      // if encrypt provided but nested type is not encrypted proto, pass through
      case Some(_) =>
        log.warn(s"skipping decrypt because message was not an EncryptedProto, ${any.typeUrl}")
        Success(any)

      // if no encryptor provided, just pass through
      case None => Success(any)
    }
  }
}

object EncryptionAdapter {
  lazy val ENCRYPTED_PROTO_NAME: String = EncryptedProto.scalaDescriptor.fullName

  /**
   * helper method to tell if a given Any message contains an instance of
   * an EncryptedProto
   *
   * @param any protobuf Any message (scalapb)
   * @return True if the any typeURL is that of an encryptedProto
   */
  def isEncryptedProto(any: Any): Boolean = {
    any.typeUrl.contains(EncryptedProto.scalaDescriptor.fullName) ||
    EncryptedProto.scalaDescriptor.fullName.contains(any.typeUrl)
  }
}