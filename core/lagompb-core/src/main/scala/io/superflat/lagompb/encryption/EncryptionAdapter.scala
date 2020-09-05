package io.superflat.lagompb.encryption

import com.google.protobuf.any.Any
import io.superflat.lagompb.protobuf.v1.core.EventWrapper
import io.superflat.lagompb.protobuf.v1.encryption.EncryptedProto
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}

/**
 * Adapter that optionally applies encryption if a ProtoEncryption is
 * provided, and otherwise passes through the provided Any message. This
 * allows users to implement a type-safe ProtoEncryption and not worry
 * about the ambiguous `Any` output.
 *
 * @param encryptor optional ProtoEncryption implementation
 */
class EncryptionAdapter(encryptor: Option[ProtoEncryption]) {

  final val log: Logger = LoggerFactory.getLogger(getClass)

  encryptor match {
    case Some(enc) => log.info(s"[Lagompb] instantiated with encryptor ${enc.getClass.getName}")
    case _         => log.info(s"[Lagompb] instantiated with no encryptor")
  }

  /**
   * encrypt adapter that only applies encryption if ProtoEncryption is provided
   *
   * @param any a scalapb Any message to encrypt
   * @return Any message optionally encrypted with provided ProtoEncryption
   */
  def encrypt(any: Any): Try[Any] =
    encryptor match {
      // if encryptor provided, attempt the encrypt
      case Some(enc) =>
        log.debug(s"[Lagompb] encrypting message ${any.typeUrl} using ${enc.getClass.getName}")
        enc.encrypt(any).map(encryptedProto => Any.pack(encryptedProto))
      // if no encryptor provided, pass through
      case None => Success(any)
    }

  /**
   * decrypt adapter that only applies decryption if ProtoEncryption is provided
   *
   * @param any the data to decrypt
   * @return
   */
  def decrypt(any: Any): Try[Any] =
    encryptor match {
      // if an encryptor provided and it's an encrypted proto, attempt decrypt
      case Some(enc) if EncryptionAdapter.isEncryptedProto(any) =>
        log.debug(s"[Lagompb] decrypting message ${any.typeUrl} using ${enc.getClass.getName}")
        Try(any.unpack(EncryptedProto)).flatMap(enc.decrypt)

      // if ProtoEncryption is configured/provided but nested type is not encrypted proto,
      // just pass original message through. this is especially useful if someone turns
      // on encryption late and has events in the journal that are not yet encrypted
      case Some(_) =>
        log.warn(s"[Lagompb] skipping decrypt because message was not an EncryptedProto, ${any.typeUrl}")
        Success(any)

      // if no encryptor provided, just pass through
      case None => Success(any)
    }

  /**
   * calls encrypt or throws error
   *
   * @param any a message to encrypt
   * @return the encrypted Any message
   */
  def encryptOrThrow(any: Any): Any =
    encrypt(any) match {
      case Success(result)    => result
      case Failure(exception) => throw exception
    }

  /**
   * calls decrypt or throws error
   *
   * @param any a message to decrypt
   * @return the decrypted Any message
   */
  def decryptOrThrow(any: Any): Any =
    decrypt(any) match {
      case Success(value)     => value
      case Failure(exception) => throw exception
    }

  /**
   * helper that decrypts the nested event and state messages
   * in an EventWrapper
   *
   * @param eventWrapper an eventwrapper with (potentially) encrypted messages
   * @return Eventwrapper with decrypted event/state or failure
   */
  def decryptEventWrapper(eventWrapper: EventWrapper): Try[EventWrapper] =
    // TODO: think about parallelizing the decrypt calls here (futures, etc)
    Try(eventWrapper)
      // decrypt the event
      .flatMap { eventWrapper =>
        this
          .decrypt(eventWrapper.getEvent)
          .map(decryptedEvent => eventWrapper.withEvent(decryptedEvent))
      }
      // decrypt the state
      .flatMap { eventWrapper =>
        this
          .decrypt(eventWrapper.getResultingState)
          .map(decryptedState => eventWrapper.withResultingState(decryptedState))
      }
}

object EncryptionAdapter {

  /**
   * helper method to tell if a given Any message contains an instance of
   * an EncryptedProto
   *
   * @param any protobuf Any message (scalapb)
   * @return True if the any typeURL is that of an encryptedProto
   */
  def isEncryptedProto(any: Any): Boolean =
    any.typeUrl.contains(EncryptedProto.scalaDescriptor.fullName) ||
      EncryptedProto.scalaDescriptor.fullName.contains(any.typeUrl)
}
