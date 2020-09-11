package io.superflat.lagompb.encryption

import com.google.protobuf.any.Any
import com.google.protobuf.wrappers.StringValue
import io.superflat.lagompb.protobuf.v1.core.{EventWrapper, MetaData}
import io.superflat.lagompb.protobuf.v1.encryption.EncryptedProto
import io.superflat.lagompb.testkit.BaseSpec

import scala.util.{Failure, Success, Try}

class EncryptionAdapterSpec extends BaseSpec {
  ".encrypt" should {
    "pass through with no ProtoEncryption provided" in {
      val adapter = new EncryptionAdapter(encryptor = None)
      val any: Any = Any.pack(StringValue("value"))
      adapter.encrypt(any) shouldBe Success(any)
    }

    "use provided ProtoEncryption" in {
      val encryptor = mock[ProtoEncryption]
      val value = StringValue("encrypted")

      val encryptedProto = EncryptedProto()
        .withEncryptedProto(value.toByteString)

      (encryptor
        .encrypt(_: Any))
        .expects(*)
        .returning(Success(encryptedProto))

      val adapter = new EncryptionAdapter(encryptor = Some(encryptor))
      val actual: Try[Any] = adapter.encrypt(Any.pack(StringValue("value")))
      val expected: Try[Any] = Success(Any.pack(encryptedProto))

      actual shouldBe expected
    }

    "handle failing encryption" in {
      val encryptor = mock[ProtoEncryption]
      val expected = Failure(new Exception("fail!"))

      (encryptor
        .encrypt(_: Any))
        .expects(*)
        .returning(expected)

      val adapter = new EncryptionAdapter(encryptor = Some(encryptor))
      val actual: Try[Any] = adapter.encrypt(Any.pack(StringValue("value")))
      actual shouldBe expected
    }
  }

  ".decrypt" should {
    "pass through with no ProtoEncryption provided" in {
      val adapter = new EncryptionAdapter(encryptor = None)
      val any: Any = Any.pack(StringValue("value"))
      adapter.decrypt(any) shouldBe Success(any)
    }

    "use provided ProtoEncryption" in {
      val encryptor = mock[ProtoEncryption]
      val value = StringValue("value")
      val any: Any = Any.pack(value)

      (encryptor
        .decrypt(_: EncryptedProto))
        .expects(*)
        .returning(Success(any))

      val adapter = new EncryptionAdapter(encryptor = Some(encryptor))
      val actual: Try[Any] = adapter.decrypt(Any.pack(EncryptedProto.defaultInstance))
      actual shouldBe Success(any)
    }

    "skip decryption if not an EncryptedProto" in {
      val encryptor = mock[ProtoEncryption]

      (encryptor
        .decrypt(_: EncryptedProto))
        .expects(*)
        .returning(Failure(new Exception("never happens")))
        .never()

      val adapter = new EncryptionAdapter(encryptor = Some(encryptor))
      val notEncrypted = Any.pack(StringValue("not encrypted"))
      val actual: Try[Any] = adapter.decrypt(notEncrypted)

      actual shouldBe Success(notEncrypted)
    }

    "handle failing decryption" in {
      val encryptor = mock[ProtoEncryption]
      val decryptFailure = Failure(new Exception("oops"))

      (encryptor
        .decrypt(_: EncryptedProto))
        .expects(*)
        .returning(decryptFailure)

      val adapter = new EncryptionAdapter(encryptor = Some(encryptor))
      val actual: Try[Any] = adapter.decrypt(Any.pack(EncryptedProto.defaultInstance))
      actual shouldBe decryptFailure
    }
  }

  ".encryptOrThrow" should {
    "pass through success" in {
      val adapter = new EncryptionAdapter(encryptor = None)
      val any: Any = Any.pack(StringValue("value"))
      Try(adapter.encryptOrThrow(any)) shouldBe Success(any)
    }

    "throw on failure" in {
      val encryptor = mock[ProtoEncryption]
      val expected = Failure(new Exception("fail!"))

      (encryptor
        .encrypt(_: Any))
        .expects(*)
        .returning(expected)

      val adapter = new EncryptionAdapter(encryptor = Some(encryptor))
      val actual: Try[Any] = Try(adapter.encryptOrThrow(Any.pack(StringValue("value"))))
      actual shouldBe expected
    }
  }

  ".decryptOrThrow" should {
    "pass through success" in {
      val adapter = new EncryptionAdapter(encryptor = None)
      val any: Any = Any.pack(StringValue("value"))
      Try(adapter.decryptOrThrow(any)) shouldBe Success(any)
    }

    "throw on failure" in {
      val encryptor = mock[ProtoEncryption]
      val decryptFailure = Failure(new Exception("oops"))

      (encryptor
        .decrypt(_: EncryptedProto))
        .expects(*)
        .returning(decryptFailure)

      val adapter = new EncryptionAdapter(encryptor = Some(encryptor))
      val actual: Try[Any] = Try(adapter.decryptOrThrow(Any.pack(EncryptedProto.defaultInstance)))
      actual shouldBe decryptFailure
    }
  }

  ".decryptEventWrapper" should {
    "decrypt nested messages" in {
      // create wrapper with event, state, and meta
      val decryptedWrapper = EventWrapper()
        .withEvent(Any.pack(StringValue("event")))
        .withResultingState(Any.pack(StringValue("state")))
        .withMeta(
          MetaData()
            .withEntityId("some-entity-id")
            .withRevisionNumber(9)
        )

      // encrypt values with NoEncryption and pack into wrapper
      val encryptedWrapper = decryptedWrapper
        .update(
          _.event := Any.pack(NoEncryption.encrypt(decryptedWrapper.getEvent).get),
          _.resultingState := Any.pack(NoEncryption.encrypt(decryptedWrapper.getResultingState).get)
        )

      // define an adapter using NoEncryption
      val adapter = new EncryptionAdapter(encryptor = Some(NoEncryption))

      val actual: Try[EventWrapper] = adapter.decryptEventWrapper(encryptedWrapper)

      // manually define expected wrapper with decrypted event & state
      actual shouldBe Success(decryptedWrapper)
    }

    "handle failed event decryption" in {

      // define mock encryptor that fails if meta contains "fail" key
      val encryptor = mock[ProtoEncryption]

      val decryptFailure = Failure(new Exception("oops"))

      (encryptor
        .decrypt(_: EncryptedProto))
        .expects(*)
        .onCall { (encryptedProto: EncryptedProto) =>
          if (encryptedProto.encryptionMeta.contains("fail"))
            decryptFailure
          else
            NoEncryption.decrypt(encryptedProto)
        }

      // encrypt values with NoEncryption and pack into wrapper
      val encryptedEvent: EncryptedProto = NoEncryption
        .encrypt(Any.pack(StringValue("event")))
        .get
        .addEncryptionMeta(("fail", "yep"))

      val encryptedState: EncryptedProto = NoEncryption
        .encrypt(Any.pack(StringValue("state")))
        .get

      val encryptedWrapper = EventWrapper()
        .withEvent(Any.pack(encryptedEvent))
        .withResultingState(Any.pack(encryptedState))

      val adapter = new EncryptionAdapter(encryptor = Some(encryptor))

      adapter.decryptEventWrapper(encryptedWrapper) shouldBe decryptFailure
    }

    "handle failed state decryption" in {
      // define mock encryptor that fails if meta contains "fail" key
      val encryptor = mock[ProtoEncryption]

      val decryptFailure = Failure(new Exception("oops"))

      (encryptor
        .decrypt(_: EncryptedProto))
        .expects(*)
        .onCall { (encryptedProto: EncryptedProto) =>
          if (encryptedProto.encryptionMeta.contains("fail"))
            decryptFailure
          else
            NoEncryption.decrypt(encryptedProto)
        }
        .twice()

      // encrypt values with NoEncryption and pack into wrapper
      val encryptedEvent: EncryptedProto = NoEncryption
        .encrypt(Any.pack(StringValue("event")))
        .get

      val encryptedState: EncryptedProto = NoEncryption
        .encrypt(Any.pack(StringValue("state")))
        .get
        .addEncryptionMeta(("fail", "yep"))

      val encryptedWrapper = EventWrapper()
        .withEvent(Any.pack(encryptedEvent))
        .withResultingState(Any.pack(encryptedState))

      val adapter = new EncryptionAdapter(encryptor = Some(encryptor))

      adapter.decryptEventWrapper(encryptedWrapper) shouldBe decryptFailure
    }
  }

  ".isEncryptedProto" should {
    "return true when Any contains an EncryptedProto" in {
      val proto = Any.pack(EncryptedProto.defaultInstance)
      EncryptionAdapter.isEncryptedProto(proto) shouldBe true
    }

    "return false when Any does not contain EncryptedProto" in {
      val proto = Any.pack(StringValue("not encrypted"))
      EncryptionAdapter.isEncryptedProto(proto) shouldBe false
    }
  }

}
