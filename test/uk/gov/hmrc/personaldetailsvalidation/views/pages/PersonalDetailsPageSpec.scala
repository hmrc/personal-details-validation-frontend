/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.personaldetailsvalidation.views.pages

import generators.Generators.Implicits._
import org.jsoup.nodes.Document
import org.scalacheck.Gen
import org.scalatestplus.play.OneAppPerSuite
import play.api.mvc.{AnyContentAsFormUrlEncoded, Request}
import setups.views.ViewSetup
import uk.gov.hmrc.personaldetailsvalidation.endpoints.routes
import uk.gov.hmrc.personaldetailsvalidation.generators.ObjectGenerators.personalDetailsObjects
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators
import uk.gov.hmrc.personaldetailsvalidation.model.CompletionUrl
import uk.gov.hmrc.play.test.UnitSpec

class PersonalDetailsPageSpec
  extends UnitSpec
    with OneAppPerSuite {

  "render" should {

    "return a personal details page containing first name, last name, nino, date of birth inputs " +
      "and a continue button" in new Setup {
      val html: Document = personalDetailsPage.render

      html.title() shouldBe messages("personal-details.title")

      html.select(".faded-text strong").text() shouldBe messages("personal-details.faded-heading")
      html.select(".faded-text ~ header h1").text() shouldBe messages("personal-details.header")
      html.select("header ~ p").text() shouldBe messages("personal-details.paragraph")

      html.select("form[method=POST]").attr("action") shouldBe routes.PersonalDetailsCollectionController.submit(completionUrl).url

      val fieldsets = html.select("form fieldset")
      val firstNameFieldset = fieldsets.first()
      firstNameFieldset.select("label[for=firstname] .form-label-bold").text() shouldBe messages("personal-details.firstname")
      firstNameFieldset.select("label[for=firstname] input[type=text][name=firstName]").isEmpty shouldBe false

      val lastNameFieldset = fieldsets.next()
      lastNameFieldset.select("label[for=lastname] .form-label-bold").text() shouldBe messages("personal-details.lastname")
      lastNameFieldset.select("label[for=lastname] input[type=text][name=lastName]").isEmpty shouldBe false

      val ninoFieldset = fieldsets.next()
      ninoFieldset.select("label[for=nino] .form-label-bold").text() shouldBe messages("personal-details.nino")
      val ninoHints = ninoFieldset.select("label[for=nino] .form-hint .form-hint")
      ninoHints.first().text() shouldBe messages("personal-details.nino.hint")
      ninoHints.next().text() shouldBe messages("personal-details.nino.hint.example")
      ninoFieldset.select("label[for=nino] input[type=text][name=nino]").isEmpty shouldBe false

      val dateFieldset = fieldsets.next().select("div")
      dateFieldset.select(".form-label-bold").text() shouldBe messages("personal-details.dateOfBirth")
      dateFieldset.select(".form-hint").text() shouldBe messages("personal-details.dateOfBirth.hint")
      val dateElementDivs = dateFieldset.select(".form-date .form-group")
      val dayElement = dateElementDivs.first()
      dayElement.select("label[for=dateOfBirth.day] span").text() shouldBe messages("personal-details.dateOfBirth.day")
      dayElement.select("label[for=dateOfBirth.day] input[type=number][name=dateOfBirth.day]").isEmpty shouldBe false
      val monthElement = dateElementDivs.next()
      monthElement.select("label[for=dateOfBirth.month] span").text() shouldBe messages("personal-details.dateOfBirth.month")
      monthElement.select("label[for=dateOfBirth.month] input[type=number][name=dateOfBirth.month]").isEmpty shouldBe false
      val yearElement = dateElementDivs.next()
      yearElement.select("label[for=dateOfBirth.year] span").text() shouldBe messages("personal-details.dateOfBirth.year")
      yearElement.select("label[for=dateOfBirth.year] input[type=number][name=dateOfBirth.year]").isEmpty shouldBe false

      html.select("form fieldset ~ div button[type=submit]").text() shouldBe messages("continue.button.text")
    }
  }

  "bindFromRequest" should {

    "return PersonalDetails when data provided on the form is valid" in new Setup with BindFromRequestTooling {

      implicit val requestWithFormData = request.withFormUrlEncodedBody(
        "firstName" -> personalDetails.firstName.toString(),
        "lastName" -> personalDetails.lastName.toString(),
        "dateOfBirth.day" -> personalDetails.dateOfBirth.getDayOfMonth.toString,
        "dateOfBirth.month" -> personalDetails.dateOfBirth.getMonthValue.toString,
        "dateOfBirth.year" -> personalDetails.dateOfBirth.getYear.toString,
        "nino" -> personalDetails.nino.toString()
      )

      val response = personalDetailsPage.bindFromRequest

      response shouldBe Right(personalDetails)
    }

    "return PersonalDetails when data provided on the form is valid but surrounded with whitespaces" in new Setup with BindFromRequestTooling {

      implicit val requestWithFormData = request.withFormUrlEncodedBody(
        "firstName" -> personalDetails.firstName.toString().surroundWithWhitespaces,
        "lastName" -> personalDetails.lastName.toString().surroundWithWhitespaces,
        "dateOfBirth.day" -> personalDetails.dateOfBirth.getDayOfMonth.toString.surroundWithWhitespaces,
        "dateOfBirth.month" -> personalDetails.dateOfBirth.getMonthValue.toString.surroundWithWhitespaces,
        "dateOfBirth.year" -> personalDetails.dateOfBirth.getYear.toString.surroundWithWhitespaces,
        "nino" -> personalDetails.nino.toString().surroundWithWhitespaces
      )

      val response = personalDetailsPage.bindFromRequest

      response shouldBe Right(personalDetails)
    }

    "return 'personal-details.firstname.required' error message " +
      "when first name is blank" in new Setup with BindFromRequestTooling {

      implicit val requestWithFormData = validRequest(replace = "firstName" -> " ")

      val Left(response) = personalDetailsPage.bindFromRequest

      val page: Document = response

      page.errorFor("firstName") shouldBe messages("personal-details.firstname.required")
    }

    "return 'personal-details.lastname.required' error message " +
      "when last name is blank" in new Setup with BindFromRequestTooling {

      implicit val requestWithFormData = validRequest(replace = "lastName" -> " ")

      val Left(response) = personalDetailsPage.bindFromRequest

      val page: Document = response

      page.errorFor("lastName") shouldBe messages("personal-details.lastname.required")
    }

    "return 'personal-details.nino.required' error message " +
      "when nino is blank" in new Setup with BindFromRequestTooling {

      implicit val requestWithFormData = validRequest(replace = "nino" -> " ")

      val Left(response) = personalDetailsPage.bindFromRequest

      val page: Document = response

      page.errorFor("nino") shouldBe messages("personal-details.nino.required")
    }

    "return 'personal-details.nino.invalid' error message " +
      "when nino is invalid" in new Setup with BindFromRequestTooling {

      implicit val requestWithFormData = validRequest(replace = "nino" -> "AA11")

      val Left(response) = personalDetailsPage.bindFromRequest

      val page: Document = response

      page.errorFor("nino") shouldBe messages("personal-details.nino.invalid")
    }

    "return 'personal-details.dateOfBirth.invalid' error message " +
      "when there's invalid date" in new Setup with BindFromRequestTooling {

      implicit val requestWithFormData = validRequest(
        replace = "dateOfBirth.day" -> "29", "dateOfBirth.month" -> "2", "dateOfBirth.year" -> "2017"
      )

      val Left(response) = personalDetailsPage.bindFromRequest

      val page: Document = response

      page.dateError shouldBe messages("personal-details.dateOfBirth.invalid")
    }

    Set("day", "month", "year") foreach { datePartName =>

      s"return 'personal-details.dateOfBirth.$datePartName.required' error message " +
        "when there's no value for day" in new Setup with BindFromRequestTooling {

        implicit val requestWithFormData = validRequest(replace = s"dateOfBirth.$datePartName" -> " ")

        val Left(response) = personalDetailsPage.bindFromRequest

        val page: Document = response

        page.dateError shouldBe messages(s"personal-details.dateOfBirth.$datePartName.required")
      }

      s"return 'personal-details.dateOfBirth.$datePartName.invalid' error message " +
        "when there's invalid value for day" in new Setup with BindFromRequestTooling {

        implicit val requestWithFormData = validRequest(replace = s"dateOfBirth.$datePartName" -> "dd")

        val Left(response) = personalDetailsPage.bindFromRequest

        val page: Document = response

        page.dateError shouldBe messages(s"personal-details.dateOfBirth.$datePartName.invalid")
      }
    }
  }

  private trait Setup extends ViewSetup {
    implicit val completionUrl: CompletionUrl = ValuesGenerators.completionUrls.generateOne

    val personalDetailsPage = new PersonalDetailsPage()
  }

  private trait BindFromRequestTooling {

    self: Setup =>

    val personalDetails = personalDetailsObjects.generateOne

    def validRequest(replace: (String, String)*): Request[AnyContentAsFormUrlEncoded] =
      request.withFormUrlEncodedBody((
        Map(
          "firstName" -> personalDetails.firstName.toString(),
          "lastName" -> personalDetails.lastName.toString(),
          "dateOfBirth.day" -> personalDetails.dateOfBirth.getDayOfMonth.toString,
          "dateOfBirth.month" -> personalDetails.dateOfBirth.getMonthValue.toString,
          "dateOfBirth.year" -> personalDetails.dateOfBirth.getYear.toString,
          "nino" -> personalDetails.nino.toString()
        ) ++ replace).toSeq: _*
      )

    implicit class ValueOps(value: String) {

      private val whitespaces: Gen[String] =
        Gen.nonEmptyListOf(Gen.const(" "))
          .map(_.mkString(""))

      lazy val surroundWithWhitespaces: String =
        s"${whitespaces.generateOne}$value${whitespaces.generateOne}"
    }

    implicit class PageOps(page: Document) {

      def errorFor(fieldName: String) = {
        val control = page.select(s"label[for=$fieldName][class=form-field--error]")
        control.isEmpty shouldBe false
        control.select(".error-notification").text()
      }

      lazy val dateError: String =
        page.select("div[class=form-date]")
          .parents()
          .first()
          .select(".error-message")
          .text()
    }
  }
}
