package io.superflat.lagompb.encryption

import io.superflat.lagompb.testkit.LagompbSpec
import io.superflat.lagompb.protobuf.core.EventWrapper
import io.superflat.lagompb.protobuf.encryption.EncryptedProto
import com.google.protobuf.wrappers.StringValue
import com.google.protobuf.any.Any
import scala.util.Try
import akka.persistence.typed.EventSeq

class EncryptedEventAdapterSpec extends LagompbSpec {

  "EncryptedEventAdapter" must {
    "safely encrypt and decrypt" in {
      val testEncryptor = new TestEncryption()
      val e: EncryptedEventAdapter = new EncryptedEventAdapter(testEncryptor)

      val event: EventWrapper = EventWrapper()
        .withEvent(Any.pack(StringValue("foo bar")))

      val encrypted: EncryptedProto = e.toJournal(event)
      val decrypted: EventSeq[EventWrapper] = e.fromJournal(encrypted, "")

      testEncryptor.didEncrypt shouldBe true
      testEncryptor.didDecrypt shouldBe true

      decrypted.events.map(_.toProtoString) shouldBe Seq(event.toProtoString)

    }

    "throw when the encrypt throws" in {
      val encryptor = new TestEncryption(shouldFail = true)
      val adapter: EncryptedEventAdapter = new EncryptedEventAdapter(encryptor)
      val event: EventWrapper = EventWrapper.defaultInstance
      val actual: Try[EncryptedProto] = Try(adapter.toJournal(event))

      actual.isFailure shouldBe true
      actual.failed.map(_.getMessage()).toOption shouldBe Some(encryptor.failureMsg)
    }

    "throw when the decrypt throws" in {
      val encryptor = new TestEncryption(shouldFail = true)
      val adapter: EncryptedEventAdapter = new EncryptedEventAdapter(encryptor)
      val proto: EncryptedProto = EncryptedProto.defaultInstance
      val actual: Try[EventSeq[EventWrapper]] = Try(adapter.fromJournal(proto, ""))

      actual.isFailure shouldBe true
      actual.failed.map(_.getMessage()).toOption shouldBe Some(encryptor.failureMsg)
    }
  }

}
