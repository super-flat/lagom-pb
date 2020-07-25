package io.superflat.lagompb.encryption

import com.google.protobuf.any.Any
import com.google.protobuf.wrappers.StringValue
import io.superflat.lagompb.protobuf.core.StateWrapper
import io.superflat.lagompb.protobuf.encryption.EncryptedProto
import io.superflat.lagompb.testkit.LagompbSpec

import scala.util.Try

class EncryptedSnapshotAdapterSpec extends LagompbSpec {

  "EncryptedSnapshotAdapter" must {
    "safely encrypt and decrypt" in {
      val encryptor = new TestEncryption()
      val adapter = new EncryptedSnapshotAdapter(encryptor)

      val state =
        StateWrapper().withState(Any.pack(StringValue("such a special state")))

      val encrypted: EncryptedProto = adapter.safeToJournal(state)
      val decrypted: StateWrapper = adapter.safeFromJournal(encrypted)

      encryptor.didEncrypt shouldBe true
      encryptor.didDecrypt shouldBe true

      decrypted.toProtoString shouldBe state.toProtoString

    }

    "throw when the encrypt throws" in {
      val encryptor = new TestEncryption(shouldFail = true)
      val adapter = new EncryptedSnapshotAdapter(encryptor)
      val state = StateWrapper.defaultInstance
      val actual: Try[EncryptedProto] = Try(adapter.safeToJournal(state))
      actual.isFailure shouldBe true
      actual.failed.map(_.getMessage()).toOption shouldBe Some(encryptor.failureMsg)
    }

    "throw when the decrypt throws" in {
      val encryptor = new TestEncryption(shouldFail = true)
      val adapter = new EncryptedSnapshotAdapter(encryptor)
      val proto = EncryptedProto.defaultInstance
      val actual: Try[StateWrapper] = Try(adapter.safeFromJournal(proto))
      actual.isFailure shouldBe true
      actual.failed.map(_.getMessage()).toOption shouldBe Some(encryptor.failureMsg)
    }
  }

}
