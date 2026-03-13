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

package uk.gov.hmrc.views

import support.UnitSpec
import uk.gov.hmrc.personaldetailsvalidation.model.DateErrorMessage

// bring form types into scope for the whole spec
import play.api.data.{Form, Forms}
import play.api.data.FormError

//VER-1008
class DateErrorMessageSpec extends UnitSpec {

  val fieldName = "fieldName"

  // helper to build a form with specific error messages
  private def formWithErrors(field: String, errorMessages: String*): Form[String] = {
    Form[String](
      Forms.single(field -> Forms.text),
      Map.empty,
      errorMessages.map(msg => FormError(field, msg)).toSeq,
      None
    )
  }

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
      "only day missing" in {
        val messages = Seq("day.required")
        DateErrorMessage.getErrorSummaryMessage(fieldName, messages) shouldBe List("day.required")
      }
      "only month missing" in {
        val messages = Seq("month.required")
        DateErrorMessage.getErrorSummaryMessage(fieldName, messages) shouldBe List("month.required")
      }
      "only year missing" in {
        val messages = Seq("year.required")
        DateErrorMessage.getErrorSummaryMessage(fieldName, messages) shouldBe List("year.required")
      }
      "all fields missing" in {
        val messages = Seq("day.required","month.required","year.required")
        DateErrorMessage.getErrorSummaryMessage(fieldName, messages) shouldBe List(s"personal-details.$fieldName.required")
      }
    }

  }

  "getDateLink" should {

    "point to the day field when day is missing" in {
      val messages = Seq("day.required")
      DateErrorMessage.getDateLink(fieldName, messages) shouldBe s"$fieldName-$fieldName.day"
    }

    "point to the day field when day and month are missing" in {
      val messages = Seq("day.required", "month.required")
      DateErrorMessage.getDateLink(fieldName, messages) shouldBe s"$fieldName-$fieldName.day"
    }

    "point to the day field when day and year are missing" in {
      val messages = Seq("day.required", "year.required")
      DateErrorMessage.getDateLink(fieldName, messages) shouldBe s"$fieldName-$fieldName.day"
    }

    "point to the month field when only month is missing" in {
      val messages = Seq("month.required")
      DateErrorMessage.getDateLink(fieldName, messages) shouldBe s"$fieldName-$fieldName.month"
    }

    "point to the month field when month and year are missing" in {
      val messages = Seq("month.required", "year.required")
      DateErrorMessage.getDateLink(fieldName, messages) shouldBe s"$fieldName-$fieldName.month"
    }

    "point to the year field when only year is missing" in {
      val messages = Seq("year.required")
      DateErrorMessage.getDateLink(fieldName, messages) shouldBe s"$fieldName-$fieldName.year"
    }

    "point to the field itself when no specific part is missing" in {
      val messages = Seq("personal-details.dateOfBirth.invalid")
      DateErrorMessage.getDateLink(fieldName, messages) shouldBe fieldName
    }

    "point to the field itself when all fields are missing" in {
      val messages = Seq("day.required", "month.required", "year.required")
      DateErrorMessage.getDateLink(fieldName, messages) shouldBe fieldName
    }
  }

  "getErrorMessage" should {


    "return the concatenated required message when day and month are missing" in {
      val form = formWithErrors(fieldName, "day.required", "month.required")
      DateErrorMessage.getErrorMessage(form, fieldName) shouldBe s"personal-details.$fieldName.miss.day.month"
    }

    "return the concatenated required message when day and year are missing" in {
      val form = formWithErrors(fieldName, "day.required", "year.required")
      DateErrorMessage.getErrorMessage(form, fieldName) shouldBe s"personal-details.$fieldName.miss.day.year"
    }

    "return the concatenated required message when month and year are missing" in {
      val form = formWithErrors(fieldName, "month.required", "year.required")
      DateErrorMessage.getErrorMessage(form, fieldName) shouldBe s"personal-details.$fieldName.miss.month.year"
    }

    "return the required message when all fields are missing" in {
      val form = formWithErrors(fieldName, "day.required", "month.required", "year.required")
      DateErrorMessage.getErrorMessage(form, fieldName) shouldBe s"personal-details.$fieldName.required"
    }

    "return the single error message when only one error is present" in {
      val form = formWithErrors(fieldName, "personal-details.dateOfBirth.invalid")
      DateErrorMessage.getErrorMessage(form, fieldName) shouldBe "personal-details.dateOfBirth.invalid"
    }

    "return empty string when there are no errors" in {
      val form = Form[String](Forms.single(fieldName -> Forms.text), Map(fieldName -> "2000-01-01"), Seq.empty, Some("2000-01-01"))
      DateErrorMessage.getErrorMessage(form, fieldName) shouldBe ""
    }
  }

}
