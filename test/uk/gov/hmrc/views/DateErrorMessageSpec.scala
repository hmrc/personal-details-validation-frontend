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

package uk.gov.hmrc.views

import support.UnitSpec
import uk.gov.hmrc.personaldetailsvalidation.model.DateErrorMessage

//VER-1008
class DateErrorMessageSpec extends UnitSpec {

  val fieldName = "fieldName"

  "getErrorSummaryMessage" should {

    "return right message" when {
      "day and month missing" in {
        val messages = Seq("day.required","month.required")
        DateErrorMessage.getErrorSummaryMessage(fieldName, messages) shouldBe List(s"personal-details.$fieldName.miss.day.month")
      }
      "day and year missing" in {
        val messages = Seq("day.required","year.required")
        DateErrorMessage.getErrorSummaryMessage(fieldName, messages) shouldBe List(s"personal-details.$fieldName.miss.day.year")
      }
      "month and year missing" in {
        val messages = Seq("month.required","year.required")
        DateErrorMessage.getErrorSummaryMessage(fieldName, messages) shouldBe List(s"personal-details.$fieldName.miss.month.year")
      }
    }

  }

}
