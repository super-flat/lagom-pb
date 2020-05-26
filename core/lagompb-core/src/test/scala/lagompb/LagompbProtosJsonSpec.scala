package lagompb

import com.google.protobuf.any.Any
import lagompb.protobuf.tests.TestAny
import lagompb.protobuf.tests.TestCmd
import lagompb.testkit.LagompbSpec
import lagompb.util.LagompbProtosJson
import org.json4s.JsonAST.JObject
import org.json4s.JValue
import org.json4s.jackson.JsonMethods.parse
import play.api.libs.{ json => pjson }
import play.api.libs.json.JsObject
import play.api.libs.json.JsResult
import play.api.libs.json.JsValue

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
           |    "@type":"type.googleapis.com/lagompb.protobuf.TestCmd",
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
           |    "@type":"type.googleapis.com/lagompb.protobuf.TestCmd",
           |    "companyUuid":"a432c2c8-1204-4a43-baa4-91eb08330b9b",
           |    "name":"test"
           |    }
           |}
           |""".stripMargin

      val jsvalue: JsValue = pjson.Json.parse(testAnyJson)
      val deserialized: JsResult[TestAny] = pjson.Json.fromJson[TestAny](jsvalue)
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
           |    "@type":"type.googleapis.com/lagompb.protobuf.TestEvent",
           |    "companyUuid":"a432c2c8-1204-4a43-baa4-91eb08330b9b",
           |    "name":"test"
           |    }
           |}
           |""".stripMargin

      val jsvalue: JsValue = pjson.Json.parse(testAnyJson)
      val deserialized: JsResult[TestAny] = pjson.Json.fromJson[TestAny](jsvalue)
      deserialized.isSuccess shouldBe false
    }

    "serialize into Json4s" in {
      val json: JsValue = pjson.Json.parse("""{"name": "pamu", "age": 1}""")
      toJson4s(json) shouldBe a[JObject]
    }

    "serialize nested into Json4s" in {
      val json: JsValue = pjson.Json.parse(s"""
                                              | {
                                              |   "firstName": "John",
                                              |   "lastName": "Smith",
                                              |   "isAlive": true,
                                              |   "age": 25,
                                              |   "address": {
                                              |     "streetAddress": "21 2nd Street",
                                              |     "city": "New York",
                                              |     "state": "NY",
                                              |     "postalCode": "10021-3100"
                                              |   },
                                              |   "phoneNumbers": [
                                              |     {
                                              |       "type": "home",
                                              |       "number": "212 555-1234"
                                              |     },
                                              |     {
                                              |       "type": "office",
                                              |       "number": "646 555-4567"
                                              |     },
                                              |     {
                                              |       "type": "mobile",
                                              |       "number": "123 456-7890"
                                              |     }
                                              |   ],
                                              |   "children": [],
                                              |   "spouse": null
                                              | }
                                              | """.stripMargin)
      toJson4s(json) shouldBe a[JObject]
    }

    "serialize into PlayJson" in {
      val json: JValue = parse("""{"name":"Toy","price":35.35}""", useBigDecimalForDouble = true)
      toPlayJson(json) shouldBe a[JsObject]

      val lotto1: JValue = parse("""{
         "lotto":{
           "lotto-id":5,
           "winning-numbers":[2,45,34,23,7,5,3],
           "winners":[{
             "winner-id":23,
             "numbers":[2,45,34,23,3,5]
           }]
         }
       }""")

      toPlayJson(lotto1) shouldBe a[JsObject]
    }
  }

}
