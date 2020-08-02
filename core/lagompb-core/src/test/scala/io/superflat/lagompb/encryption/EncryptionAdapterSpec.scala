package io.superflat.lagompb.encryption

import com.google.protobuf.any.Any
import com.google.protobuf.wrappers.StringValue
import com.google.protobuf.ByteString
import io.superflat.lagompb.protobuf.encryption.EncryptedProto
import io.superflat.lagompb.testkit.BaseSpec

import scala.util.Success

class EncryptionAdapterSpec extends BaseSpec {
  ".encrypt" should {
    "pass through with no ProtoEncryption provided" in {}

    "use provided ProtoEncryption" in {}

    "handle failing encryption" in {}
  }

  ".decrypt" should {
    "pass through with no ProtoEncryption provided" in {}

    "use provided ProtoEncryption" in {}

    "skip decryption if not an EncryptedProto" in {}

    "handle failing decryption" in {}
  }

  ".isEncryptedProto" should {
    "return true when Any contains an EncryptedProto" in {}

    "return false when Any does not contain EncryptedProto" in {}
  }

}
