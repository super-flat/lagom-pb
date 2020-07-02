package io.superflat.lagompb.encryption

import com.google.protobuf.any.Any
import io.superflat.lagompb.protobuf.core.StateWrapper
import io.superflat.lagompb.protobuf.encryption.EncryptedProto

import scala.util.{Failure, Success}

/**
 * implements the TypesafeSnapshotAdapter by applying a ProtoEncryption
 * implementation to encrypt/decrypt snapshots
 *
 * @param encryptor an implementation of ProtoEncryption
 */
class EncryptedSnapshotAdapter(encryptor: ProtoEncryption)
    extends TypesafeSnapshotAdapter[StateWrapper, EncryptedProto] {

  /**
   * given a state wrapper, encrypt and return an EncryptedProto
   *
   * @param state some StateWrapper instance
   * @return an EncryptedProto
   */
  def safeToJournal(state: StateWrapper): EncryptedProto = {
    encryptor.encrypt(Any.pack(state)) match {
      case Success(value) => value
      case Failure(exception) => throw exception
    }
  }

  /**
   * given an EncryptedProto, use the provided ProtoEncryption implementation
   * to decrypt and return the StateWrapper.
   *
   * @param from some EncryptedProto instance
   * @return a StateWrapper instance
   */
  def safeFromJournal(from: EncryptedProto): StateWrapper = {
    encryptor.decrypt(from) match {
      case Success(value) => value.unpack[StateWrapper]
      case Failure(exception) => throw exception
    }
  }
}
