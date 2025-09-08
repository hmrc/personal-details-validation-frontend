/*
 * Copyright 2025 HM Revenue & Customs
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

import uk.gov.hmrc.personaldetailsvalidation.model.InitialPersonalDetailsForm
import uk.gov.hmrc.personaldetailsvalidation.utils.ComponentSpecHelper

import java.time.LocalDate

class InitialPersonalDetailsFormISpec extends ComponentSpecHelper {

  "InitialPersonalDetailsForm" should {

    "bind Welsh abbreviated month names (case-insensitive)" in {
      val welshAbbrMonths: Seq[(String, Int)] = Seq(
        "ION" -> 1, "CHWEF" -> 2, "MAW" -> 3, "EBR" -> 4, "MAI" -> 5, "MEH" -> 6,
        "GORFF" -> 7, "AWST" -> 8, "MEDI" -> 9, "HYD" -> 10, "TACH" -> 11, "RHAG" -> 12
      )

      welshAbbrMonths.foreach { case (abbr, monthNum) =>
        val data = Map(
          "firstName" -> "Aled",
          "lastName" -> "Cymru",
          "dateOfBirth.year" -> "1990",
          "dateOfBirth.month" -> abbr.toLowerCase, // mixed/lower case accepted
          "dateOfBirth.day" -> "15"
        )

        val bound = InitialPersonalDetailsForm.initialForm.bind(data)
        bound.errors shouldBe Nil
        val dob = bound.get.dateOfBirth
        dob shouldBe LocalDate.of(1990, monthNum, 15)
      }
    }

    "bind Welsh full month names (case-insensitive)" in {
      val welshFullMonths: Seq[(String, Int)] = Seq(
        "IONAWR" -> 1, "CHWEFROR" -> 2, "MAWRTH" -> 3, "EBRILL" -> 4, "MAI" -> 5, "MEHEFIN" -> 6,
        "GORFFENNAF" -> 7, "AWST" -> 8, "MEDI" -> 9, "HYDREF" -> 10, "TACHWEDD" -> 11, "RHAGFYR" -> 12
      )

      welshFullMonths.foreach { case (full, monthNum) =>
        val data = Map(
          "firstName" -> "Aled",
          "lastName" -> "Cymru",
          "dateOfBirth.year" -> "1985",
          "dateOfBirth.month" -> full.capitalize, // mixed case accepted
          "dateOfBirth.day" -> "15"
        )

        val bound = InitialPersonalDetailsForm.initialForm.bind(data)
        bound.errors shouldBe Nil
        val dob = bound.get.dateOfBirth
        dob shouldBe LocalDate.of(1985, monthNum, 15)
      }
    }
  }
}
