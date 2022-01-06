/*
 * Copyright 2022 HM Revenue & Customs
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

import support.UnitSpec

class PostcodeDetailsSpecs extends UnitSpec {

  "Postcode Regex validation should work as expected" should {
    "validate valid postcodes" in {
      PostcodeDetailsForm.postcodeFormatValidation(NonEmptyString("BN12 4XH")) shouldBe true
      PostcodeDetailsForm.postcodeFormatValidation(NonEmptyString("bn12 4xh")) shouldBe true //lowercase also ok
      PostcodeDetailsForm.postcodeFormatValidation(NonEmptyString("L13 1xy")) shouldBe true
      PostcodeDetailsForm.postcodeFormatValidation(NonEmptyString("J1 2FE")) shouldBe true
    }
    "not validate invalid postcodes that will fail on the address lookup service" in {
      PostcodeDetailsForm.postcodeFormatValidation(NonEmptyString("BN12   4XH")) shouldBe false //can't have more than 1 space
      PostcodeDetailsForm.postcodeFormatValidation(NonEmptyString("J1 22FE")) shouldBe false //can't have 2 numbers in 2nd part
      PostcodeDetailsForm.postcodeFormatValidation(NonEmptyString("CR 2JJ")) shouldBe false //first part doesn't end with number
      PostcodeDetailsForm.postcodeFormatValidation(NonEmptyString("J1 2F")) shouldBe false //2nd part doesn't end in 2 letters
    }
  }
}
