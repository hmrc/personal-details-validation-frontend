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

import play.api.libs.json._
import uk.gov.voa.valuetype.tooling.UnitSpec
import uk.gov.voa.valuetype.{TestIntOption, TestLongOption, TestStringOption}

class OptionsFormatSpec extends UnitSpec {

  import OptionsFormat._

  "optionsFormat for StringOptions" should {

    import TestStringOption._

    implicit val format = optionsFormat(TestStringOption)

    "allow serialization of the given option to a JsString with value of the given option's value" in {
      Json.toJson(TestOption1) shouldBe JsString("1")
    }

    "allow deserialization of the given JsString to a relevant Option" in {
      Json.fromJson(JsString("2")) shouldBe JsSuccess(TestOption2)
    }

    "return a deserialization error when deserializing a value for a non-exisitent option" in {
      Json.fromJson(JsString("x")) shouldBe a [JsError]
    }

    "return a deserialization error when deserializing a non-string value" in {
      implicit val format = optionsFormat(TestStringOption)
      Json.fromJson(JsNumber(1)) shouldBe a [JsError]
    }
  }

  "optionsFormat for IntOptions" should {

    import TestIntOption._

    implicit val format = optionsFormat(TestIntOption)

    "allow serialization of the given option to a JsNumber with value of the given option's value" in {
      Json.toJson(TestOption7) shouldBe JsNumber(7)
    }

    "allow deserialization of the given JsNumber to a relevant Option" in {
      Json.fromJson(JsNumber(5)) shouldBe JsSuccess(TestOption5)
    }

    "return a deserialization error when deserializing a value for a non-existent option" in {
      Json.fromJson(JsNumber(0)) shouldBe a [JsError]
    }

    "return a deserialization error when deserializing a non-integer numeric value" in {
      implicit val format = optionsFormat(TestIntOption)
      Json.fromJson(JsNumber(5.1)) shouldBe a [JsError]
    }

    "return a deserialization error when deserializing an integer numeric value that is too large" in {
      implicit val format = optionsFormat(TestIntOption)
      Json.fromJson(JsNumber(Long.MaxValue)) shouldBe a [JsError]
    }

    "return a deserialization error when deserializing a non-numeric value" in {
      implicit val format = optionsFormat(TestIntOption)
      Json.fromJson(JsString("5")) shouldBe a [JsError]
    }
  }

  "optionsFormat for LongOptions" should {

    import TestLongOption._

    implicit val format = optionsFormat(TestLongOption)

    "allow serialization of the given option to a JsNumber with value of the given option's value" in {
      Json.toJson(TestOption7) shouldBe JsNumber(7)
    }

    "allow deserialization of the given JsNumber to a relevant Option" in {
      Json.fromJson(JsNumber(5)) shouldBe JsSuccess(TestOption5)
    }

    "return a deserialization error when deserializing a value for a non-exisitent option" in {
      Json.fromJson(JsNumber(0)) shouldBe a [JsError]
    }

    "return a deserialization error when deserializing a non-integer numeric value" in {
      implicit val format = optionsFormat(TestLongOption)
      Json.fromJson(JsNumber(5.1)) shouldBe a [JsError]
    }

    "return a deserialization error when deserializing a non-numeric value" in {
      implicit val format = optionsFormat(TestLongOption)
      Json.fromJson(JsString("5")) shouldBe a [JsError]
    }
  }
}
