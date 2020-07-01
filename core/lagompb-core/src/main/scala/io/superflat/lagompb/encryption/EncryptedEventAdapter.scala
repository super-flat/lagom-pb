package io.superflat.lagompb.encryption

import akka.persistence.typed.{EventAdapter, EventSeq}
import com.google.protobuf.any.Any
import com.google.protobuf.ByteString
import io.superflat.lagompb.protobuf.core.EventWrapper
import io.superflat.lagompb.protobuf.encryption.EncryptedProto
import io.superflat.lagompb.encryption.ProtoEncryption
import scala.util.{Failure, Success, Try}

/**
 * Akka persistence event adaptor that encrypts persisted events and
 * decryptes events read from the journal.
 *
 * @param encryptor ProtoEncryption implementation for encrypt/decrypt
 */
class EncryptedEventAdapter(encryptor: ProtoEncryption) extends EventAdapter[EventWrapper, EncryptedProto] {

  /**
   * given an event wrapper instance, return an EncryptedProto by applying the
   * provided ProtoEncryption .encrypt operation
   *
   * @param e some EventWrapper instance (scalapb protobuf class)
   * @return EncryptedProto instance
   */
  override def toJournal(e: EventWrapper): EncryptedProto = {
    encryptor.encrypt(Any.pack(e)) match {
      case Success(value) => value
      case Failure(exception) => throw exception
    }
  }

  /**
   * given an encrypted proto, attempt to decrypt the message using provided
   * ProtoEncryption implementation and return an EventWrapper
   *
   * Note: Due to the signature of the parent, this can only express failed
   * decrypt opreations by throwing an error.
   * type
   *
   * @param p an EncryptedProto instance
   * @param manifest string manifest to describe this adaptere
   * @return unpacked EventWrapper
   */
  override def fromJournal(p: EncryptedProto, manifest: String): EventSeq[EventWrapper] = {
    val someAny: Any = encryptor.decrypt(p) match {
      case Success(value) => value
      case Failure(exception) => throw exception
    }

    val eventWrapper: EventWrapper = someAny.unpack(EventWrapper)

    EventSeq.single(eventWrapper)
  }

  /**
   * returns the manifest for this adapter (currently not used)
   *
   * @param event some EventWrapper instance
   * @return string manifest
   */
  override def manifest(event: EventWrapper): String = ""
}
