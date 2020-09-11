package io.superflat.lagompb.encryption

import com.google.protobuf.ByteString
import com.google.protobuf.any.Any
import com.google.protobuf.wrappers.StringValue
import io.superflat.lagompb.protobuf.v1.encryption.EncryptedProto
import io.superflat.lagompb.testkit.BaseSpec

import scala.util.Success

class NoEncryptionSpec extends BaseSpec {

  "NoEncryption" must {
    "encrypt with no loss" in {
      val proto: Any = Any.pack(StringValue("some message to encrypt"))
      val encrypted = NoEncryption.encrypt(proto)
      val expected = EncryptedProto().withEncryptedProto(proto.toByteString)

      encrypted.map(_.toProtoString) shouldBe Success(expected.toProtoString)
    }

    "decrypt with no loss" in {
      val msg: Any = Any.pack(StringValue("some message to decrypt"))
      val byteString: ByteString = msg.toByteString
      val encrypted = EncryptedProto().withEncryptedProto(byteString)
      val decrypted = NoEncryption.decrypt(encrypted)

      decrypted.map(_.toProtoString) shouldBe Success(msg.toProtoString)
    }
  }

}
