package lagompb

import com.google.protobuf.any.Any
import lagompb.tests.{TestAny, TestCmd}
import lagompb.testkit.LagompbSpec
import lagompb.util.LagompbProtosJson
import play.api.libs.json.{JsResult, JsValue}
import play.api.libs.{json => pjson}

class LagompbProtosJsonSpec extends LagompbSpec with LagompbProtosJson {
  val companyUuid = "a432c2c8-1204-4a43-baa4-91eb08330b9b"

  "Api Serializer" should {
    "serialize registered companion" in {
      val testCmd = TestCmd().withCompanyUuid(companyUuid).withName("test")
      val testCmdJson =
        s"""
           |{
           |"companyUuid":"a432c2c8-1204-4a43-baa4-91eb08330b9b",
           |"name":"test"
           |}
           |""".stripMargin
      val serialized: JsValue = play.api.libs.json.Json.toJson(testCmd)
      serialized should ===(play.api.libs.json.Json.parse(testCmdJson))
    }

    "serialize a registered companion with Any" in {
      val testCmd = TestCmd().withCompanyUuid(companyUuid).withName("test")
      val testAny = TestAny().withCmd(Any.pack(testCmd))

      val testAnyJson =
        s"""
           |{
           |"cmd":{
           |    "@type":"type.googleapis.com/lagompb.TestCmd",
           |    "companyUuid":"a432c2c8-1204-4a43-baa4-91eb08330b9b",
           |    "name":"test"
           |    }
           |}
           |""".stripMargin
      val serialized: JsValue = play.api.libs.json.Json.toJson(testAny)
      serialized should ===(play.api.libs.json.Json.parse(testAnyJson))
    }

    "deserialize correctly if its respective companion is registered" in {
      val testAnyJson =
        s"""
           |{
           |"cmd":{
           |    "@type":"type.googleapis.com/lagompb.TestCmd",
           |    "companyUuid":"a432c2c8-1204-4a43-baa4-91eb08330b9b",
           |    "name":"test"
           |    }
           |}
           |""".stripMargin

      val jsvalue: JsValue = pjson.Json.parse(testAnyJson)
      val deserialized: JsResult[TestAny] =
        pjson.Json.fromJson[TestAny](jsvalue)
      deserialized.isSuccess shouldBe true

      val testCmdJson =
        s"""
           |{
           |"companyUuid":"a432c2c8-1204-4a43-baa4-91eb08330b9b",
           |"name":"test"
           |}
           |""".stripMargin

      val parsed: JsValue = pjson.Json.parse(testCmdJson)
      val des: JsResult[TestCmd] = pjson.Json.fromJson[TestCmd](parsed)
      des.isSuccess shouldBe true
    }

    "fail to deserialize if its respective any data companion is not registered" in {
      val testAnyJson =
        s"""
           |{
           |"TestCmd":{
           |    "@type":"type.googleapis.com/lagompb.TestEvent",
           |    "companyUuid":"a432c2c8-1204-4a43-baa4-91eb08330b9b",
           |    "name":"test"
           |    }
           |}
           |""".stripMargin

      val jsvalue: JsValue = pjson.Json.parse(testAnyJson)
      val deserialized: JsResult[TestAny] =
        pjson.Json.fromJson[TestAny](jsvalue)
      deserialized.isSuccess shouldBe false
    }
  }

}
