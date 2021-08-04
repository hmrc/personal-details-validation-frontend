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

package uk.gov.hmrc.personaldetailsvalidation.views.pages

import generators.Generators.Implicits._
import org.jsoup.nodes.Document
import org.scalacheck.Gen
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.mvc.{AnyContentAsFormUrlEncoded, Cookie, Request}
import setups.views.ViewSetup
import support.UnitSpec
import uk.gov.hmrc.config.{AppConfig, DwpMessagesApiProvider}
import uk.gov.hmrc.personaldetailsvalidation.endpoints.routes
import uk.gov.hmrc.personaldetailsvalidation.generators.ObjectGenerators._
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators
import uk.gov.hmrc.personaldetailsvalidation.model.CompletionUrl
import uk.gov.hmrc.personaldetailsvalidation.views.html.template.{personal_details_nino, personal_details_postcode}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.collection.JavaConverters._

class PersonalDetailsPageSpec extends UnitSpec with GuiceOneAppPerSuite {

  "render" should {

    "return a personal details page containing first name, last name, nino, date of birth inputs " +
      "and a continue button" in new Setup {
      val html: Document = personalDetailsPage.render(postCodePageRequested = false)
      html.title() shouldBe s"${messages("personal-details.title")} - GOV.UK"

      html.select("span.govuk-caption-xl").text() shouldBe messages("personal-details.faded-heading")
      html.select("h1.govuk-heading-l").text() shouldBe messages("personal-details.header")
      html.select("h1 ~ p.govuk-body").text() shouldBe messages("personal-details.paragraph")

      html.select("form[method=POST]").attr("action") shouldBe routes.PersonalDetailsCollectionController.submit(completionUrl).url

      html.select(".govuk-error-summary__list li").isEmpty shouldBe true

      val fieldsets = html.select("form .govuk-form-group")

      val firstNameFieldset = fieldsets.first()
      firstNameFieldset.select("label[for=firstName]").text() shouldBe messages("personal-details.firstname")
      firstNameFieldset.select("input[type=text][name=firstName]").isEmpty shouldBe false

      val lastNameFieldset = fieldsets.next()
      lastNameFieldset.select("label[for=lastName]").text() shouldBe messages("personal-details.lastname")
      lastNameFieldset.select("input[type=text][name=lastName]").isEmpty shouldBe false

      val ninoFieldset = fieldsets.next()
      ninoFieldset.select("label[for=nino]").text() shouldBe messages("personal-details.nino")
      
      val ninoHints = ninoFieldset.select("#nino-hint.govuk-hint")
      ninoHints.first().text() contains messages("personal-details.nino.hint")
      ninoFieldset.select("input[type=text][name=nino]").isEmpty shouldBe false

      val dateFieldset = html.select(".govuk-form-group.date-of-birth-wrapper fieldset")
      dateFieldset.select("legend").text() shouldBe messages("personal-details.dateOfBirth")
      dateFieldset.select(".govuk-hint").text() shouldBe messages("personal-details.dateOfBirth.hint")
      val dateElementDivs = dateFieldset.select("div.govuk-date-input .govuk-date-input__item")

      val dayElement = dateElementDivs.first()
      dayElement.select("label[for=dateOfBirth-dateOfBirth.day]").text() shouldBe messages("personal-details.dateOfBirth.day")
      dayElement.select("input[type=text][name=dateOfBirth.day]").isEmpty shouldBe false

      val monthElement = dateElementDivs.next()
      monthElement.select("label[for=dateOfBirth-dateOfBirth.month]").text() shouldBe messages("personal-details.dateOfBirth.month")
      monthElement.select("input[type=text][name=dateOfBirth.month]").isEmpty shouldBe false

      val yearElement = dateElementDivs.next()
      yearElement.select("label[for=dateOfBirth-dateOfBirth.year]").text() shouldBe messages("personal-details.dateOfBirth.year")
      yearElement.select("input[type=text][name=dateOfBirth.year]").isEmpty shouldBe false

      html.select("button[type=submit]").text() shouldBe messages("continue.button.text")
    }

    "return a personal details page containing first name, last name, postcode, date of birth inputs " +
      "and a continue button" in new Setup {
      val html: Document = personalDetailsPage.render(postCodePageRequested = true)

      html.title() shouldBe messages("personal-details.title") + " - GOV.UK"

      html.select("span.govuk-caption-xl").text() shouldBe messages("personal-details.faded-heading")
      html.select("h1.govuk-heading-l").text() shouldBe messages("personal-details.header")
      html.select("h1 ~ p.govuk-body").text() shouldBe messages("personal-details.paragraph")

      html.select("form[method=POST]").attr("action") shouldBe routes.PersonalDetailsCollectionController.submit(completionUrl, true).url

      html.select(".govuk-error-summary__list li").isEmpty shouldBe true

      val formGroup = html.select("form .govuk-form-group")

      val firstNameFormGroup = formGroup.first()
      firstNameFormGroup.select("label[for=firstName]").text() shouldBe messages("personal-details.firstname")
      firstNameFormGroup.select("input[type=text][name=firstName]").isEmpty shouldBe false

      val lastNameFormGroup = formGroup.next()
      lastNameFormGroup.select("label[for=lastName]").text() shouldBe messages("personal-details.lastname")
      lastNameFormGroup.select("input[type=text][name=lastName]").isEmpty shouldBe false

      val postcodeFormGroup = formGroup.next()
      postcodeFormGroup.select("label[for=postcode]").text() shouldBe messages("personal-details.postcode")

      val postcodeHints = postcodeFormGroup.select("#postcode-hint.govuk-hint")
      postcodeHints.first().text() shouldBe messages("personal-details.postcode.hint")
      postcodeFormGroup.select("input[type=text][name=postcode]").isEmpty shouldBe false

      val dateFormGroup = html.select(".govuk-form-group.date-of-birth-wrapper fieldset")

      dateFormGroup.select("legend").text() shouldBe messages("personal-details.dateOfBirth")
      dateFormGroup.select(".govuk-hint").text() shouldBe messages("personal-details.dateOfBirth.hint")

      val dateElementDivs = dateFormGroup.select(".govuk-date-input__item")

      val dayElement = dateElementDivs.first()
      dayElement.select("label[for=dateOfBirth-dateOfBirth.day]").text() shouldBe messages("personal-details.dateOfBirth.day")
      dayElement.select("input[type=text][name=dateOfBirth.day]").isEmpty shouldBe false

      val monthElement = dateElementDivs.next()
      monthElement.select("label[for=dateOfBirth-dateOfBirth.month]").text() shouldBe messages("personal-details.dateOfBirth.month")
      monthElement.select("input[type=text][name=dateOfBirth.month]").isEmpty shouldBe false

      val yearElement = dateElementDivs.next()
      yearElement.select("label[for=dateOfBirth-dateOfBirth.year]").text() shouldBe messages("personal-details.dateOfBirth.year")
      yearElement.select("input[type=text][name=dateOfBirth.year]").isEmpty shouldBe false

      html.select("form button").text() shouldBe messages("continue.button.text")
    }

    "return a personal details page containing first name, last name, nino, date of birth inputs " +
      "and include a link to postcode when asking for nino" in new Setup {
      val html: Document = personalDetailsPage.render(postCodePageRequested = false)

      html.title() shouldBe messages("personal-details.title") + " - GOV.UK"

      val fieldsets = html.select("form .govuk-form-group")
      val firstNameFieldset = fieldsets.get(0)
      firstNameFieldset.select("label").text() shouldBe messages("personal-details.firstname")

      val lastNameFieldset = fieldsets.get(1)
      lastNameFieldset.select("label").text() shouldBe messages("personal-details.lastname")

      val ninoFieldset = fieldsets.get(2)
      ninoFieldset.select("label").first().text() shouldBe messages("personal-details.nino")
      html.select("#nino-unknown").size() shouldBe 1

      val dateFieldset = html.select(".govuk-form-group.date-of-birth-wrapper fieldset")
      dateFieldset.select("legend").text() shouldBe messages("personal-details.dateOfBirth")
    }

    "return a personal details page containing first name, last name, post code, date of birth inputs " +
      "and a continue button when asking for postcode" in new Setup {
      val html: Document = personalDetailsPage.render(postCodePageRequested = true)

      html.title() shouldBe messages("personal-details.title") + " - GOV.UK"

      val formGroups = html.select("form .govuk-form-group")
      val firstNameFormGroup = formGroups.get(0)
      firstNameFormGroup.select("label").text() shouldBe messages("personal-details.firstname")

      val lastNameFormGroup = formGroups.get(1)
      lastNameFormGroup.select("label").text() shouldBe messages("personal-details.lastname")

      val postcodeFormGroup = formGroups.get(2)
      postcodeFormGroup.select("label").text() shouldBe messages("personal-details.postcode")

      val dateFieldset = html.select(".govuk-fieldset")
      dateFieldset.select("legend").text() shouldBe messages("personal-details.dateOfBirth")
    }
  }

  "renderValidationFailure" should {

    "return a personal details page containing first name, last name, nino, date of birth inputs " +
      "and a continue button and validation error" in new Setup {
      val html: Document = personalDetailsPage.renderValidationFailure(postCodePageRequested = false)

      html.title() shouldBe s"Error: ${messages("personal-details.title")} - GOV.UK"

      html.select("span.govuk-caption-xl").text() shouldBe messages("personal-details.faded-heading")
      html.select("h1.govuk-heading-l").text() shouldBe messages("personal-details.header")
      html.select("h1 ~ p.govuk-body").text() shouldBe messages("personal-details.paragraph")

      html.select("form[method=POST]").attr("action") shouldBe routes.PersonalDetailsCollectionController.submit(completionUrl).url

      val errors = html.select(".govuk-error-summary__list li").asScala.map(_.text()).toList
      errors shouldBe List("We could not find any records that match the details you entered. Please try again.")

      val fieldsets = html.select("form .govuk-form-group")
      val firstNameFieldset = fieldsets.first()
      firstNameFieldset.select("label[for=firstName]").text() shouldBe messages("personal-details.firstname")
      firstNameFieldset.select("input[type=text][name=firstName]").isEmpty shouldBe false

      val lastNameFieldset = fieldsets.next()
      lastNameFieldset.select("label[for=lastName]").text() shouldBe messages("personal-details.lastname")
      lastNameFieldset.select("input[type=text][name=lastName]").isEmpty shouldBe false

      val ninoFieldset = fieldsets.next()
      ninoFieldset.select("label[for=nino]").text() shouldBe messages("personal-details.nino")
      val ninoHints = ninoFieldset.select("#nino-hint.govuk-hint")
      ninoHints.first().text() contains messages("personal-details.nino.hint")
      ninoFieldset.select("input[type=text][name=nino]").isEmpty shouldBe false

      val dateFieldset = html.select(".govuk-form-group.date-of-birth-wrapper fieldset")

      dateFieldset.select("legend").text() shouldBe messages("personal-details.dateOfBirth")
      dateFieldset.select("#dateOfBirth-hint").text() shouldBe messages("personal-details.dateOfBirth.hint")

      val dateElementDivs = dateFieldset.select(".govuk-date-input__item")
      val dayElement = dateElementDivs.first()
      dayElement.select("label[for=dateOfBirth-dateOfBirth.day]").text() shouldBe messages("personal-details.dateOfBirth.day")
      dayElement.select("input[type=text][name=dateOfBirth.day]").isEmpty shouldBe false

      val monthElement = dateElementDivs.next()
      monthElement.select("label[for=dateOfBirth-dateOfBirth.month]").text() shouldBe messages("personal-details.dateOfBirth.month")
      monthElement.select("input[type=text][name=dateOfBirth.month]").isEmpty shouldBe false

      val yearElement = dateElementDivs.next()
      yearElement.select("label[for=dateOfBirth-dateOfBirth.year]").text() shouldBe messages("personal-details.dateOfBirth.year")
      yearElement.select("input[type=text][name=dateOfBirth.year]").isEmpty shouldBe false

      html.select("button[type=submit]").text() shouldBe messages("continue.button.text")
    }

    "return a personal details page containing first name, last name, postcode, date of birth inputs " +
      "and a continue button and validation error" in new Setup {
      val html: Document = personalDetailsPage.renderValidationFailure(postCodePageRequested = true)

      html.title() shouldBe s"Error: ${messages("personal-details.title")} - GOV.UK"

      html.select("h1").text() shouldBe messages("personal-details.header")
      html.select("h1 ~ p.govuk-body").text() shouldBe messages("personal-details.paragraph")

      html.select("form[method=POST]").attr("action") shouldBe routes.PersonalDetailsCollectionController.submit(completionUrl, true).url

      val errors = html.select(".govuk-error-summary__list").asScala.map(_.text()).toList
      errors shouldBe List("We could not find any records that match the details you entered. Please try again.")

      val formGroup = html.select("form .govuk-form-group")
      val firstNameFormGroup = formGroup.first()
      firstNameFormGroup.select("label[for=firstName]").text() shouldBe messages("personal-details.firstname")
      firstNameFormGroup.select("input[type=text][name=firstName]").isEmpty shouldBe false

      val lastNameFormGroup = formGroup.next()
      lastNameFormGroup.select("label[for=lastName]").text() shouldBe messages("personal-details.lastname")
      lastNameFormGroup.select("input[type=text][name=lastName]").isEmpty shouldBe false

      val postcodeFormGroup = formGroup.next()
      postcodeFormGroup.select("label[for=postcode]").text() shouldBe messages("personal-details.postcode")
      val postcodeHints = postcodeFormGroup.select("#postcode-hint.govuk-hint")
      postcodeHints.first().text() shouldBe messages("personal-details.postcode.hint")
      postcodeFormGroup.select("input[type=text][name=postcode]").isEmpty shouldBe false

      val dateFieldset = html.select(".govuk-fieldset")
      val dateFormGroup = dateFieldset.first()

      dateFormGroup.select(".govuk-fieldset__legend").text() shouldBe messages("personal-details.dateOfBirth")
      dateFormGroup.select("#dateOfBirth-hint.govuk-hint").text() shouldBe messages("personal-details.dateOfBirth.hint")
      val dateElementDivs = dateFormGroup.select(".govuk-date-input .govuk-date-input__item")
      val dayElement = dateElementDivs.first()
      dayElement.select("label[for=dateOfBirth-dateOfBirth.day]").text() shouldBe messages("personal-details.dateOfBirth.day")
      dayElement.select("input[type=text][name=dateOfBirth.day]").isEmpty shouldBe false
      val monthElement = dateElementDivs.next()
      monthElement.select("label[for=dateOfBirth-dateOfBirth.month]").text() shouldBe messages("personal-details.dateOfBirth.month")
      monthElement.select("input[type=text][name=dateOfBirth.month]").isEmpty shouldBe false
      val yearElement = dateElementDivs.next()
      yearElement.select("label[for=dateOfBirth-dateOfBirth.year]").text() shouldBe messages("personal-details.dateOfBirth.year")
      yearElement.select("input[type=text][name=dateOfBirth.year]").isEmpty shouldBe false

      html.select("form button").text() shouldBe messages("continue.button.text")
    }

    "return a personal details page containing DWP validation error and validationId link if journeyOrigin is 'dwp-iv" in new Setup with BindFromRequestTooling {

      implicit val requestWithFormData = validRequestDwp(replace = "nino" -> "AA123456A")

      val html: Document = personalDetailsPage.renderValidationFailure(postCodePageRequested = false)

      html.title() shouldBe s"Error: ${messages("personal-details.title")} - GOV.UK"

      val errors = html.select(".govuk-error-summary__list li").asScala.map(_.text()).toList
      errors shouldBe List("We could not find any records that match the details you entered. Please try again, or confirm your identity another way")
    }

    "return a personal details page containing HMRC validation error if journeyOrigin is NOT 'dwp-iv" in new Setup with BindFromRequestTooling {

      implicit val requestWithFormData = validRequestHMRC(replace = "nino" -> "AA123456A")

      val html: Document = personalDetailsPage.renderValidationFailure(postCodePageRequested = false)

      html.title() shouldBe s"Error: ${messages("personal-details.title")} - GOV.UK"

      html.toString should not include(viewConfig.dwpGetHelpUrl)

      val errors = html.select(".govuk-error-summary__list li").asScala.map(_.text()).toList
      errors shouldBe List("We could not find any records that match the details you entered. Please try again.")
    }

    "return a personal details page containing DWP validation error in welsh if journeyOrigin is 'dwp-iv' and language is welsh" in new Setup with BindFromRequestTooling {

      override implicit val messages = welshMessages

      implicit val requestWithFormData = validRequestDwpCy(replace = "nino" -> "AA123456A")

      val html: Document = personalDetailsPage.renderValidationFailure(postCodePageRequested = false)

      html.title() shouldBe s"${welshMessages("error.prefix")} ${welshMessages("personal-details.title")} - GOV.UK"

      val errors = html.select(".govuk-error-summary__list li").asScala.map(_.text()).toList
      errors shouldBe List("Nid oeddem yn gallu dod o hyd i unrhyw gofnodion sy’n cyd-fynd â’r manylion a nodwyd gennych. Rhowch gynnig arall arni, neu cadarnhewch pwy ydych gan ddefnyddio dull arall")
    }

    "return a personal details page containing first name, last name, nino, date of birth inputs " +
      "and include a link to postcode when asking for nino" in new Setup {
      val html: Document = personalDetailsPage.renderValidationFailure(postCodePageRequested = false)

      html.title() shouldBe s"Error: ${messages("personal-details.title")} - GOV.UK"

      val fieldsets = html.select("form .govuk-form-group")
      val firstNameFieldset = fieldsets.get(0)
      firstNameFieldset.select("label").text() shouldBe messages("personal-details.firstname")

      val lastNameFieldset = fieldsets.get(1)
      lastNameFieldset.select("label").text() shouldBe messages("personal-details.lastname")

      val ninoFieldset = fieldsets.get(2)
      ninoFieldset.select("label").first().text() shouldBe messages("personal-details.nino")
      html.select("#nino-unknown").size() shouldBe 1

      val dateFieldset = html.select(".govuk-form-group.date-of-birth-wrapper fieldset")
      dateFieldset.select("legend").text() shouldBe messages("personal-details.dateOfBirth")
    }

    "return a personal details page containing first name, last name, post code, date of birth inputs " +
      "and a continue button when asking for postcode" in new Setup {
      val html: Document = personalDetailsPage.renderValidationFailure(postCodePageRequested = true)

      html.title() shouldBe s"Error: ${messages("personal-details.title")} - GOV.UK"

      val formGroups = html.select("form .govuk-form-group")
      val firstNameFormGroup = formGroups.get(0)
      firstNameFormGroup.select("label").text() shouldBe messages("personal-details.firstname")

      val lastNameFormGroup = formGroups.get(1)
      lastNameFormGroup.select("label").text() shouldBe messages("personal-details.lastname")

      val postcodeFormGroup = formGroups.get(2)
      postcodeFormGroup.select("label").text() shouldBe messages("personal-details.postcode")

      val dateFieldset = html.select(".govuk-fieldset")
      dateFieldset.select("legend").text() shouldBe messages("personal-details.dateOfBirth")
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

      val response = personalDetailsPage.bindFromRequest(postCodePageRequested = false)

      response shouldBe Right(personalDetails)
    }

    "return PersonalDetails when data provided on the form is valid but nino is in lowercase" in new Setup with BindFromRequestTooling {

      implicit val requestWithFormData = request.withFormUrlEncodedBody(
        "firstName" -> personalDetails.firstName.toString(),
        "lastName" -> personalDetails.lastName.toString(),
        "dateOfBirth.day" -> personalDetails.dateOfBirth.getDayOfMonth.toString,
        "dateOfBirth.month" -> personalDetails.dateOfBirth.getMonthValue.toString,
        "dateOfBirth.year" -> personalDetails.dateOfBirth.getYear.toString,
        "nino" -> personalDetails.nino.toString().toLowerCase
      )

      val response = personalDetailsPage.bindFromRequest(postCodePageRequested = false)

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

      val response = personalDetailsPage.bindFromRequest(false)

      response shouldBe Right(personalDetails)
    }

    "return PersonalDetails with Postcode when data provided on the form is valid" in new Setup with BindFromRequestTooling {
      implicit val requestWithFormData = request.withFormUrlEncodedBody(
        "firstName" -> personalDetailsWithPostcode.firstName.toString(),
        "lastName" -> personalDetailsWithPostcode.lastName.toString(),
        "dateOfBirth.day" -> personalDetailsWithPostcode.dateOfBirth.getDayOfMonth.toString,
        "dateOfBirth.month" -> personalDetailsWithPostcode.dateOfBirth.getMonthValue.toString,
        "dateOfBirth.year" -> personalDetailsWithPostcode.dateOfBirth.getYear.toString,
        "postcode" -> personalDetailsWithPostcode.postCode.toString()
      )

      val response = personalDetailsPage.bindFromRequest(true)

      response shouldBe Right(personalDetailsWithPostcode)
    }

    "return PersonalDetails with Postcode when data provided on the form is valid but surrounded with whitespaces" in new Setup with BindFromRequestTooling {
      implicit val requestWithFormData = request.withFormUrlEncodedBody(
        "firstName" -> personalDetailsWithPostcode.firstName.toString().surroundWithWhitespaces,
        "lastName" -> personalDetailsWithPostcode.lastName.toString().surroundWithWhitespaces,
        "dateOfBirth.day" -> personalDetailsWithPostcode.dateOfBirth.getDayOfMonth.toString.surroundWithWhitespaces,
        "dateOfBirth.month" -> personalDetailsWithPostcode.dateOfBirth.getMonthValue.toString.surroundWithWhitespaces,
        "dateOfBirth.year" -> personalDetailsWithPostcode.dateOfBirth.getYear.toString.surroundWithWhitespaces,
        "postcode" -> personalDetailsWithPostcode.postCode.toString().surroundWithWhitespaces
      )

      val response = personalDetailsPage.bindFromRequest(true)

      response shouldBe Right(personalDetailsWithPostcode)
    }

    "return 'personal-details.firstname.required' error message " +
      "when first name is blank" in new Setup with BindFromRequestTooling {

      implicit val requestWithFormData = validRequest(replace = "firstName" -> " ")

      val Left(response) = personalDetailsPage.bindFromRequest(false)

      val page: Document = response

      page.errorsSummary.heading shouldBe messages("error-summary.heading")
      page.errorsSummary.content shouldBe messages("personal-details.firstname.required")

      page.errorFor("firstName") shouldBe "Error: " + messages("personal-details.firstname.required")
    }

    "return 'personal-details.lastname.required' error message " +
      "when last name is blank" in new Setup with BindFromRequestTooling {

      implicit val requestWithFormData = validRequest(replace = "lastName" -> " ")

      val Left(response) = personalDetailsPage.bindFromRequest(false)

      val page: Document = response

      page.errorsSummary.heading shouldBe messages("error-summary.heading")
      page.errorsSummary.content shouldBe messages("personal-details.lastname.required")

      page.errorFor("lastName") shouldBe  "Error: " + messages("personal-details.lastname.required")
    }

    "return 'personal-details.nino.required' error message " +
      "when nino is blank" in new Setup with BindFromRequestTooling {
      implicit val requestWithFormData = validRequest(replace = "nino" -> " ")

      val Left(response) = personalDetailsPage.bindFromRequest(false)

      val page: Document = response

      page.errorsSummary.heading shouldBe messages("error-summary.heading")
      page.errorsSummary.content shouldBe messages("personal-details.nino.required")
    }

    List("LE2 OAJ", "AO1 9KK", "N7 0f8", "D8 JJ") foreach { invalidPostcode =>

      "return 'personal-details.postcode.invalid' error message " +
        s"when postcode $invalidPostcode contains invalid characters" in new Setup with BindFromRequestTooling {
        implicit val requestWithFormData = validRequestWithPostcode(replace = "postcode" -> invalidPostcode)

        val Left(response) = personalDetailsPage.bindFromRequest(true)

        val page: Document = response

        page.errorsSummary.heading shouldBe messages("error-summary.heading")
        page.errorSpan("postcode") shouldBe "<span class=\"govuk-visually-hidden\">Error:</span> " + messages("personal-details.postcode.invalid")

      }
    }

    "return 'personal-details.nino.invalid' error message " +
      "when nino is invalid" in new Setup with BindFromRequestTooling {

      implicit val requestWithFormData = validRequest(replace = "nino" -> "AA11")

      val Left(response) = personalDetailsPage.bindFromRequest(false)

      val page: Document = response

      page.errorsSummary.heading shouldBe messages("error-summary.heading")
      page.errorsSummary.content shouldBe messages("personal-details.nino.invalid")

      page.errorSpan("nino") shouldBe "<span class=\"govuk-visually-hidden\">Error:</span> " + messages("personal-details.nino.invalid")
    }

    "return 'personal-details.dateOfBirth.required' error message " +
      "when any of the date parts are given" in new Setup with BindFromRequestTooling {

      implicit val requestWithFormData = validRequest(
        replace = "dateOfBirth.day" -> " ", "dateOfBirth.month" -> "", "dateOfBirth.year" -> ""
      )

      val Left(response) = personalDetailsPage.bindFromRequest(false)

      val page: Document = response

      page.errorsSummary.heading shouldBe messages("error-summary.heading")
      page.errorsSummary.content shouldBe messages("personal-details.dateOfBirth.required")

      page.dateError shouldBe "Error: " +messages("personal-details.dateOfBirth.required")
    }

    "return 'personal-details.dateOfBirth.invalid' error message " +
      "when there's invalid date" in new Setup with BindFromRequestTooling {

      implicit val requestWithFormData = validRequest(
        replace = "dateOfBirth.day" -> "29", "dateOfBirth.month" -> "2", "dateOfBirth.year" -> "2017"
      )

      val Left(response) = personalDetailsPage.bindFromRequest(false)

      val page: Document = response

      page.errorsSummary.heading shouldBe messages("error-summary.heading")
      page.errorsSummary.content shouldBe messages("personal-details.dateOfBirth.invalid")

      page.dateError shouldBe "Error: " + messages("personal-details.dateOfBirth.invalid")
    }

    Set("day", "month", "year") foreach { datePartName =>

      s"return 'personal-details.dateOfBirth.$datePartName.required' error message " +
        "when there's no value for day" in new Setup with BindFromRequestTooling {

        implicit val requestWithFormData = validRequest(replace = s"dateOfBirth.$datePartName" -> " ")

        val Left(response) = personalDetailsPage.bindFromRequest(false)

        val page: Document = response

        page.errorsSummary.heading shouldBe messages("error-summary.heading")
        page.errorsSummary.content shouldBe messages(s"personal-details.dateOfBirth.$datePartName.required")

        page.dateError shouldBe "Error: " + messages(s"personal-details.dateOfBirth.$datePartName.required")
      }

      s"return 'personal-details.dateOfBirth.$datePartName.invalid' error message " +
        "when there's invalid value for day" in new Setup with BindFromRequestTooling {

        implicit val requestWithFormData = validRequest(replace = s"dateOfBirth.$datePartName" -> "dd")

        val Left(response) = personalDetailsPage.bindFromRequest(false)

        val page: Document = response

        page.errorsSummary.heading shouldBe messages("error-summary.heading")
        page.errorsSummary.content shouldBe messages(s"personal-details.dateOfBirth.$datePartName.invalid")

        page.dateError shouldBe "Error: " + messages(s"personal-details.dateOfBirth.$datePartName.invalid")
      }
    }

    "return 'Nino' error page, with link, when not requesting postcode" in new Setup with BindFromRequestTooling {
      implicit val requestWithFormData = validRequest(replace = "firstName" -> " ")

      val Left(response) = personalDetailsPage.bindFromRequest(postCodePageRequested = false)

      val page: Document = response

      page.select("label[for=nino]").text() shouldBe messages("personal-details.nino")
    }

    "return 'PostCode' error page when requesting postcode" in new Setup with BindFromRequestTooling {
      implicit val requestWithFormData = validRequest(replace = "firstName" -> " ")

      val Left(response) = personalDetailsPage.bindFromRequest(postCodePageRequested = true)

      val page: Document = response

      val pcField = page.select(".govuk-form-group").get(2)
      pcField.select(".govuk-label").first().text() shouldBe messages("personal-details.postcode")
    }
  }

  private trait Setup extends ViewSetup {

    implicit val completionUrl: CompletionUrl = ValuesGenerators.completionUrls.generateOne
    lazy val testConfig: Map[String, Any] = Map.empty

    lazy val appConfig = new AppConfig(Configuration.from(testConfig), mock[ServicesConfig])

    implicit val mockDwpMessagesApi = app.injector.instanceOf[DwpMessagesApiProvider]

    private val personal_details_postcode: personal_details_postcode = app
      .injector.instanceOf[personal_details_postcode]

    private val personal_details_nino: personal_details_nino = app.injector
      .instanceOf[personal_details_nino]

    val personalDetailsPage = new PersonalDetailsPage(
      appConfig,
      personal_details_postcode,
      personal_details_nino
    )
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
      ).withSession(request.session + "loginOrigin" -> "dwp-iv-some-variant")

    def validRequestHMRC(replace: (String, String)*): Request[AnyContentAsFormUrlEncoded] =
      request.withFormUrlEncodedBody((
        Map(
          "firstName" -> personalDetails.firstName.toString(),
          "lastName" -> personalDetails.lastName.toString(),
          "dateOfBirth.day" -> personalDetails.dateOfBirth.getDayOfMonth.toString,
          "dateOfBirth.month" -> personalDetails.dateOfBirth.getMonthValue.toString,
          "dateOfBirth.year" -> personalDetails.dateOfBirth.getYear.toString,
          "nino" -> personalDetails.nino.toString()
        ) ++ replace).toSeq: _*
      ).withSession(request.session + "loginOrigin" -> "ma")

    def validRequestDwp(replace: (String, String)*): Request[AnyContentAsFormUrlEncoded] =
      request.withFormUrlEncodedBody((
        Map(
          "firstName" -> personalDetails.firstName.toString(),
          "lastName" -> personalDetails.lastName.toString(),
          "dateOfBirth.day" -> personalDetails.dateOfBirth.getDayOfMonth.toString,
          "dateOfBirth.month" -> personalDetails.dateOfBirth.getMonthValue.toString,
          "dateOfBirth.year" -> personalDetails.dateOfBirth.getYear.toString,
          "nino" -> personalDetails.nino.toString()
        ) ++ replace).toSeq: _*
      ).withSession("loginOrigin" -> "dwp-iv-some-variant")

    def validRequestDwpCy(replace: (String, String)*): Request[AnyContentAsFormUrlEncoded] =
      request.withFormUrlEncodedBody((
        Map(
          "firstName" -> personalDetails.firstName.toString(),
          "lastName" -> personalDetails.lastName.toString(),
          "dateOfBirth.day" -> personalDetails.dateOfBirth.getDayOfMonth.toString,
          "dateOfBirth.month" -> personalDetails.dateOfBirth.getMonthValue.toString,
          "dateOfBirth.year" -> personalDetails.dateOfBirth.getYear.toString,
          "nino" -> personalDetails.nino.toString()
        ) ++ replace).toSeq: _*
      ).withSession("loginOrigin" -> "dwp-iv-some-variant").withCookies(Cookie("PLAY_LANG", "cy"))


    val personalDetailsWithPostcode = personalDetailsObjectsWithPostcode.generateOne

    def validRequestWithPostcode(replace: (String, String)*): Request[AnyContentAsFormUrlEncoded] =
      request.withFormUrlEncodedBody((
        Map(
          "firstName" -> personalDetailsWithPostcode.firstName.toString(),
          "lastName" -> personalDetailsWithPostcode.lastName.toString(),
          "dateOfBirth.day" -> personalDetailsWithPostcode.dateOfBirth.getDayOfMonth.toString,
          "dateOfBirth.month" -> personalDetailsWithPostcode.dateOfBirth.getMonthValue.toString,
          "dateOfBirth.year" -> personalDetailsWithPostcode.dateOfBirth.getYear.toString,
          "postcode" -> personalDetailsWithPostcode.postCode.toString
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

      lazy val errorsSummary = new {

        private lazy val errorsSummaryDiv =
          page.select("div[class=flash error-summary error-summary--show],.govuk-error-summary")

        lazy val heading = errorsSummaryDiv.select("h2").text()

        lazy val content = errorsSummaryDiv.select("ul").text()
      }

      def errorFor(fieldName: String): String = {
        val cssSelector = s"#${fieldName}-error.govuk-error-message"
        val control = page.select(cssSelector)
        control.isEmpty shouldBe false
        val text = control.text()
        text
      }

      def errorSpan(fieldName: String): String = {
        page.select(s"label[for=$fieldName]")
          .parents()
          .first()
          .select(".govuk-error-message")
          .html()
      }

      lazy val dateError: String =
        page.select("span#dateOfBirth-error")
          .text()
    }
  }
}
