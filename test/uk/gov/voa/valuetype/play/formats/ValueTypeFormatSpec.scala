/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.voa.valuetype.play.formats

import play.api.libs.json.Json.{fromJson, toJson}
import play.api.libs.json._
import uk.gov.voa.valuetype._
import uk.gov.voa.valuetype.play.formats.ValueTypeFormat._
import uk.gov.voa.valuetype.tooling.UnitSpec

class StringValueTypeFormatSpec extends UnitSpec {

  implicit val stringValueFormat = format(TestStringValue.apply)

  "StringValue.format" should {

    "allow to deserialize given value to json" in {
      toJson(TestStringValue("value")) shouldBe JsString("value")
    }

    "allow deserializing json into an object" in {
      fromJson(JsString("value")).get shouldBe TestStringValue("value")
    }

    "fail deserialization when given json has the wrong type" in {
      fromJson(JsNumber(1)) shouldBe a[JsError]
    }
  }
}

class IntValueTypeFormatSpec extends UnitSpec {

  implicit val intValueFormat = format(TestIntValue.apply)

  "IntValue.format" should {

    "allow to serialize given value to json" in {
      toJson(TestIntValue(1)) shouldBe JsNumber(1)
    }

    "allow deserializing json into an object" in {
      fromJson(JsNumber(1)).get shouldBe TestIntValue(1)
    }

    "fail deserialization when given json has the wrong type" in {
      fromJson(JsString("1")) shouldBe a[JsError]
    }

    "fail deserialization if the value is not an integer" in {
      fromJson(JsNumber(1.1)) shouldBe a[JsError]
    }

    "fail deserialization if the value is not a number" in {
      JsString("1").validate[TestIntValue] shouldBe a[JsError]
    }

    "fail deserialization if the value is out of range" in {
      fromJson(JsNumber(1L + Int.MaxValue.toLong)) shouldBe a[JsError]
    }
  }
}

class LongValueTypeFormatSpec extends UnitSpec {

  implicit val longValueFormat = format(TestLongValue.apply)

  "LongValue.format" should {

    "allow to serialize given value to json" in {
      toJson(TestLongValue(1)) shouldBe JsNumber(1)
    }

    "allow deserializing json into an object" in {
      fromJson(JsNumber(1)).get shouldBe TestLongValue(1)
      fromJson(JsNumber(1L + Int.MaxValue.toLong)).get shouldBe TestLongValue(1L + Int.MaxValue.toLong)
    }

    "fail deserialization if the value is not an integer" in {
      JsNumber(1.1).validate[TestLongValue] shouldBe a[JsError]
    }

    "fail deserialization if the value is not a number" in {
      JsString("1").validate[TestLongValue] shouldBe a[JsError]
    }
  }
}

class BooleanValueTypeFormatSpec extends UnitSpec {

  implicit val booleanValueFormat = format(TestBooleanValue.apply)

  "BooleanValue.format" should {

    "allow to serialize given value to json" in {
      toJson(TestBooleanValue(true)) shouldBe JsBoolean(true)
    }

    "allow deserializing json into an object" in {
      fromJson(JsBoolean(false)).get shouldBe TestBooleanValue(false)
    }

    "fail deserialization when given json has the wrong type" in {
      fromJson(JsNumber(0)) shouldBe a[JsError]
    }
  }
}

class BigDecimalValueTypeFormatSpec extends UnitSpec {

  implicit val bigDecimalValueFormat = format(TestBigDecimalValue.apply)

  "BigDecimalValue.format" should {

    "allow to serialize given value to json" in {
      toJson(TestBigDecimalValue(2.1051)) shouldBe JsNumber(2.1051)
    }

    "allow deserializing json into an object" in {
      fromJson(JsNumber(2.1051)).get shouldBe TestBigDecimalValue(2.1051)
    }

    "fail deserialization when given json has the wrong type" in {
      fromJson[TestBigDecimalValue](JsString("1")) shouldBe a[JsError]
    }
  }

}

class RoundedBigDecimalValueTypeFormatSpec extends UnitSpec {

  implicit val bigDecimalValueFormat = format(TestRoundedBigDecimalValue.apply)

  "BigDecimalValue.format" should {

    "allow to serialize given value to json" in {
      toJson(TestRoundedBigDecimalValue(2.105)) shouldBe JsNumber(2.11)
    }

    "allow deserializing json into an object" in {
      fromJson(JsNumber(2.105)).get shouldBe TestRoundedBigDecimalValue(2.11)
    }

    "fail deserialization when given json has the wrong type" in {
      fromJson[TestRoundedBigDecimalValue](JsString("1")) shouldBe a[JsError]
    }
  }
}

class ValueTypeFormatSpec extends UnitSpec {

  "valueTypeReadsFor" should {

    "allow to deserialize given json into an object" in {

      implicit val stringValueReads = valueTypeReadsFor(TestStringValue.apply)

      fromJson(JsString("value")).get shouldBe TestStringValue("value")
    }

  }

  "valueTypeWritesFor" should {

    "allow to serialize given object into a json" in {

      implicit val stringValueReads = valueTypeWritesFor[Int, TestIntValue]

      toJson(TestIntValue(1)) shouldBe JsNumber(1)
    }

    "allow to serialize given StringValue object into a json" in {

      implicit val stringValueReads = valueTypeWritesFor[TestStringValue]

      toJson(TestStringValue("value")) shouldBe JsString("value")
    }

  }

  "serializing a value type" should {

    "produce the correct JSON" in new TestCase {
      toJson(Weekend("Saturday")) shouldBe JsString("Saturday")
    }
  }

  "deserializing a value type" should {

    "produce the correct object for valid JSON" in new TestCase {
      fromJson(JsString("Sunday")).get shouldBe Weekend("Sunday")
    }

    "fail when given JSON has the wrong type" in new TestCase {
      fromJson[Weekend](JsNumber(2)) shouldBe a[JsError]
      fromJson[Weekend](JsBoolean(true)) shouldBe a[JsError]
      fromJson[Weekend](Json.arr("Saturday", "Sunday")) shouldBe a[JsError]
      fromJson[Weekend](Json.obj("day" -> "Saturday")) shouldBe a[JsError]
    }

    "fail when given JSON contains an invalid value" in new TestCase {
      fromJson[Weekend](JsString("Monday")) shouldBe a[JsError]
    }
  }

  private trait TestCase {

    case class Weekend(value: String) extends ValueType[String] {
      require(value == "Saturday" || value == "Sunday")
    }

    implicit val weekendFormat: Format[Weekend] = format(Weekend)
  }

}
