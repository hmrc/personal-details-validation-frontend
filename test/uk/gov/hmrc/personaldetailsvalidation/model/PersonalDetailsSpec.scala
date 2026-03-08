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

import java.time.LocalDate

class PersonalDetailsSpec extends UnitSpec {

  private val firstName  = NonEmptyString("John")
  private val lastName   = NonEmptyString("Smith")
  private val dob        = LocalDate.of(1990, 1, 1)

  "PersonalDetailsWithNino" should {

    "have journeyVersion set to NINO" in {
      val details = PersonalDetailsWithNino(firstName, lastName, Nino("AA000003D"), dob)
      details.journeyVersion shouldBe "NINO"
    }

    "store the provided fields" in {
      val nino    = Nino("AA000003D")
      val details = PersonalDetailsWithNino(firstName, lastName, nino, dob)
      details.firstName  shouldBe firstName
      details.lastName   shouldBe lastName
      details.nino       shouldBe nino
      details.dateOfBirth shouldBe dob
    }
  }

  "PersonalDetailsWithPostcode" should {

    "have journeyVersion set to Postcode" in {
      val details = PersonalDetailsWithPostcode(firstName, lastName, NonEmptyString("SW1A 1AA"), dob)
      details.journeyVersion shouldBe "Postcode"
    }

    "store the provided fields" in {
      val postcode = NonEmptyString("SW1A 1AA")
      val details  = PersonalDetailsWithPostcode(firstName, lastName, postcode, dob)
      details.firstName   shouldBe firstName
      details.lastName    shouldBe lastName
      details.postCode    shouldBe postcode
      details.dateOfBirth shouldBe dob
    }
  }
}
