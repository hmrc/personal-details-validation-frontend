/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.personaldetailsvalidation.model

import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import support.{TestStringValue, UnitSpec}

class StringValueSpec extends UnitSpec with PropertyChecks {

  val generatedStrings = Gen.alphaStr

  "StringValue" should {

    "provide type name" in {
      case class NestedType(value: String) extends StringValue

      NestedType("vcalue").typeName shouldBe "NestedType"
    }
  }

  "StringValue.apply" should {

    "work for any String" in {
      forAll(generatedStrings) { value =>
        TestStringValue(value).value shouldBe value
      }
    }
  }

  "StringValue.toString" should {

    "be the same as the given value" in {
      forAll(generatedStrings) { value =>
        TestStringValue(value).toString shouldBe value
      }
    }
  }
}
