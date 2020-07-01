package io.superflat.lagompb.encryption

import io.superflat.lagompb.protobuf.encryption.EncryptedProto
import io.superflat.lagompb.encryption.ProtoEncryption
import io.superflat.lagompb.protobuf.core.StateWrapper
import akka.persistence.typed.SnapshotAdapter
import com.google.protobuf.any.Any

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
    encryptor
      .encrypt(Any.pack(state))
      .get
  }

  /**
   * given an EncryptedProto, use the provided ProtoEncryption implementation
   * to decrypt and return the StateWrapper.
   *
   * @param from some EncryptedProto instance
   * @return a StateWrapper instance
   */
  def safeFromJournal(from: EncryptedProto): StateWrapper = {
    encryptor
      .decrypt(from)
      .map(_.unpack[StateWrapper])
      .get
  }
}
