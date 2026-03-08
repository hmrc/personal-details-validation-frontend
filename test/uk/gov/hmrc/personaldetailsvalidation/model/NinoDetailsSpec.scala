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

package uk.gov.hmrc.personaldetailsvalidation.model

import support.UnitSpec
import uk.gov.hmrc.domain.Nino

class NinoDetailsSpec extends UnitSpec {

  "NinoDetailsForm" should {

    "bind successfully for a valid NINO" in {
      val bound = NinoDetailsForm.ninoForm.bind(Map("nino" -> "AA000003D"))

      bound.errors shouldBe Nil
      bound.value  shouldBe Some(NinoDetails(Nino("AA000003D")))
    }

    "normalise NINO to uppercase and strip spaces" in {
      val bound = NinoDetailsForm.ninoForm.bind(Map("nino" -> "aa 00 00 03 d"))

      bound.errors shouldBe Nil
      bound.value.map(_.nino.value) shouldBe Some("AA000003D")
    }

    "return an error when the NINO field is empty" in {
      val bound = NinoDetailsForm.ninoForm.bind(Map("nino" -> ""))

      bound.errors should not be empty
    }

    "return an error when the NINO field is absent" in {
      val bound = NinoDetailsForm.ninoForm.bind(Map.empty[String, String])

      bound.errors should not be empty
    }

    "return an error for a malformed NINO" in {
      val bound = NinoDetailsForm.ninoForm.bind(Map("nino" -> "INVALID"))

      bound.errors should not be empty
      bound.errors.head.message shouldBe "personal-details.nino.invalid"
    }

    "unbind a NinoDetails back to a form data map" in {
      val ninoDetails = NinoDetails(Nino("AA000003D"))
      val unbound = NinoDetailsForm.ninoForm.mapping.unbind(ninoDetails)

      unbound shouldBe Map("nino" -> "AA000003D")
    }
  }
}
