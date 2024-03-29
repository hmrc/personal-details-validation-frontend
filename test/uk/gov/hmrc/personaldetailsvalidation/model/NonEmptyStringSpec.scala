/*
 * Copyright 2023 HM Revenue & Customs
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
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import support.UnitSpec

class NonEmptyStringSpec extends UnitSpec with ScalaCheckDrivenPropertyChecks {

  "apply" should {

    "return a NonEmptyString for non-empty Strings" in {
      forAll(Gen.alphaNumStr.suchThat(_.trim.nonEmpty)) { string =>
        NonEmptyString(string).value shouldBe string
      }
    }

    "return Left with an IllegalArgumentException for an empty String" in {
      intercept[IllegalArgumentException](NonEmptyString(""))
        .getMessage should endWith("NonEmptyString cannot be empty")
    }

    "return Left with an IllegalArgumentException for a blank String" in {
      intercept[IllegalArgumentException](NonEmptyString(" "))
        .getMessage should endWith("NonEmptyString cannot be empty")
    }
  }
}
