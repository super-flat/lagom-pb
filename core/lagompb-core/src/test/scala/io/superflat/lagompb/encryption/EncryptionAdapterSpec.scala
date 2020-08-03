package io.superflat.lagompb.encryption

import com.google.protobuf.any.Any
import com.google.protobuf.wrappers.StringValue
import com.google.protobuf.ByteString
import io.superflat.lagompb.protobuf.encryption.EncryptedProto
import io.superflat.lagompb.testkit.BaseSpec
import org.scalamock.scalatest.MockFactory

import scala.util.{Failure, Success, Try}

class EncryptionAdapterSpec extends BaseSpec {
  ".encrypt" should {
    "pass through with no ProtoEncryption provided" in {
      val adapter = new EncryptionAdapter(encryptor = None)
      val any: Any = Any.pack(StringValue("value"))
      adapter.encrypt(any) shouldBe (Success(any))
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

      actual shouldBe (expected)
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
      actual shouldBe (expected)
    }
  }

  ".decrypt" should {
    "pass through with no ProtoEncryption provided" in {
      val adapter = new EncryptionAdapter(encryptor = None)
      val any: Any = Any.pack(StringValue("value"))
      adapter.decrypt(any) shouldBe (Success(any))
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
      actual shouldBe (Success(any))
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

      actual shouldBe (Success(notEncrypted))
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
      actual shouldBe (decryptFailure)
    }
  }

  ".isEncryptedProto" should {
    "return true when Any contains an EncryptedProto" in {
      val proto = Any.pack(EncryptedProto.defaultInstance)
      EncryptionAdapter.isEncryptedProto(proto) shouldBe (true)
    }

    "return false when Any does not contain EncryptedProto" in {
      val proto = Any.pack(StringValue("not encrypted"))
      EncryptionAdapter.isEncryptedProto(proto) shouldBe (false)
    }
  }

}
