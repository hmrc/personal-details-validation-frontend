/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.personaldetailsvalidation

import uk.gov.hmrc.personaldetailsvalidation.model.{NonEmptyString, PostcodeDetails, PostcodeDetailsForm}
import uk.gov.hmrc.personaldetailsvalidation.utils.ComponentSpecHelper

class PostcodeDetailsFormISpec extends ComponentSpecHelper {

  "PostcodeDetailsForm" should {

    "bind a valid UK postcode" in {
      val bound = PostcodeDetailsForm.postcodeForm.bind(Map("postcode" -> "SW1A 1AA"))

      bound.errors shouldBe Nil
      bound.value  shouldBe Some(PostcodeDetails(NonEmptyString("SW1A 1AA")))
    }

    "bind a valid UK postcode in lowercase" in {
      val bound = PostcodeDetailsForm.postcodeForm.bind(Map("postcode" -> "bn12 4xh"))

      bound.errors shouldBe Nil
    }

    "bind a postcode with internal spaces" in {
      val bound = PostcodeDetailsForm.postcodeForm.bind(Map("postcode" -> "B   N 1 24  X H"))

      bound.errors shouldBe Nil
    }

    "return an error for a missing postcode field" in {
      val bound = PostcodeDetailsForm.postcodeForm.bind(Map.empty[String, String])

      bound.errors should not be empty
    }

    "return an error for an empty postcode" in {
      val bound = PostcodeDetailsForm.postcodeForm.bind(Map("postcode" -> ""))

      bound.errors should not be empty
    }

    "return an error for a postcode that does not match the required format" in {
      val bound = PostcodeDetailsForm.postcodeForm.bind(Map("postcode" -> "INVALID"))

      bound.errors should not be empty
      bound.errors.head.message shouldBe "personal-details.postcode.invalid"
    }

    "return an error for a postcode where the second part has two consecutive digits" in {
      val bound = PostcodeDetailsForm.postcodeForm.bind(Map("postcode" -> "J13 22FE"))

      bound.errors should not be empty
    }

    "unbind a PostcodeDetails back to a map" in {
      val details = PostcodeDetails(NonEmptyString("SW1A 1AA"))
      val data    = PostcodeDetailsForm.postcodeForm.fill(details).data

      data.get("postcode") shouldBe Some("SW1A 1AA")
    }
  }
}
