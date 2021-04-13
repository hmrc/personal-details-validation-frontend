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

package uk.gov.hmrc.personaldetailsvalidation.endpoints

import java.time.LocalDate
import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.data._
import cats.implicits.catsStdInstancesForFuture
import generators.Generators.Implicits._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito
import org.scalacheck.Gen
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages}
import play.api.mvc.Results._
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import scalamock.AsyncMockArgumentMatchers
import setups.controllers.ResultVerifiers._
import support.UnitSpec
import uk.gov.hmrc.config.{AppConfig, DwpMessagesApiProvider}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.personaldetailsvalidation.generators.ObjectGenerators.{personalDetailsObjects, personalDetailsObjectsWithPostcode}
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators
import uk.gov.hmrc.personaldetailsvalidation.model._
import uk.gov.hmrc.personaldetailsvalidation.monitoring.{EventDispatcher, TimedOut, TimeoutContinue}
import uk.gov.hmrc.personaldetailsvalidation.views.html.template.{enter_your_details_nino, enter_your_details_postcode, personal_details_main}
import uk.gov.hmrc.personaldetailsvalidation.views.pages.PersonalDetailsPage
import uk.gov.hmrc.views.ViewConfig

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class PersonalDetailsCollectionControllerSpec
  extends UnitSpec
    with AsyncMockFactory
    with AsyncMockArgumentMatchers
    with GuiceOneAppPerSuite
    with ScalaFutures {

  "showPage" should {

    "return OK with HTML body rendered using PersonalDetailsPage when the multi page flag is disabled" in new Setup {

      (pageMock.render(_: Boolean)(_: CompletionUrl, _: Request[_]))
        .expects(false, completionUrl, request)
        .returning(Html("content"))

      (mockAppConfig.isMultiPageEnabled _).expects().returns(false)

      val result = controller.showPage(completionUrl, alternativeVersion = false)(request)

      verify(result).has(statusCode = OK, content = "content")
    }

    "return OK with simplified first page when the multi page flag is enabled" in new Setup {
      (mockAppConfig.isMultiPageEnabled _).expects().returns(true)

      val result = controller.showPage(completionUrl, alternativeVersion = false)(request)

      status(result) shouldBe OK
      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document = Jsoup.parse(contentAsString(result))

      document.select("h1.heading-xlarge").text() shouldBe messages("personal-details.faded-heading") + " " + messages("personal-details.header")
      document.select("h1.heading-xlarge ~ p").text() shouldBe messages("personal-details.paragraph")

      document.select("form[method=POST]").attr("action") shouldBe routes.PersonalDetailsCollectionController.submitMainDetails(completionUrl).url

      document.select("#error-summary-display .js-error-summary-messages").isEmpty shouldBe true

      val backButton = document.select("#identifiersBackLink")
      backButton.text() shouldBe messages("button.back.text")
      backButton.attr("href") shouldBe "javascript:history.back()"

      val fieldsets = document.select("form .form-group")
      val firstNameFieldset = fieldsets.first()
      firstNameFieldset.select("label[for=firstname]").text() shouldBe messages("personal-details.firstname")
      firstNameFieldset.select("label[for=firstname] input[type=text][name=firstName]").isEmpty shouldBe false

      val lastNameFieldset = fieldsets.next()
      lastNameFieldset.select("label[for=lastname]").text() shouldBe messages("personal-details.lastname")
      lastNameFieldset.select("label[for=lastname] input[type=text][name=lastName]").isEmpty shouldBe false

      val dateFieldset = fieldsets.next().select("fieldset")
      dateFieldset.select(".form-label-bold").text() shouldBe messages("personal-details.dateOfBirth")
      dateFieldset.select(".form-hint").text() shouldBe messages("personal-details.dateOfBirth.hint")
      val dateElementDivs = dateFieldset.select(".form-date .form-group")
      val dayElement = dateElementDivs.first()
      dayElement.select("label[for=dateOfBirth.day] span").text() shouldBe messages("personal-details.dateOfBirth.day")
      dayElement.select("label[for=dateOfBirth.day] input[type=text][name=dateOfBirth.day]").isEmpty shouldBe false
      val monthElement = dateElementDivs.next()
      monthElement.select("label[for=dateOfBirth.month] span").text() shouldBe messages("personal-details.dateOfBirth.month")
      monthElement.select("label[for=dateOfBirth.month] input[type=text][name=dateOfBirth.month]").isEmpty shouldBe false
      val yearElement = dateElementDivs.next()
      yearElement.select("label[for=dateOfBirth.year] span").text() shouldBe messages("personal-details.dateOfBirth.year")
      yearElement.select("label[for=dateOfBirth.year] input[type=text][name=dateOfBirth.year]").isEmpty shouldBe false

      document.select("button[type=submit]").text() shouldBe messages("continue.button.text")
    }

    "return OK with simplified first page, containing data from session, when the multi page flag is enabled" in new Setup {
      (mockAppConfig.isMultiPageEnabled _).expects().returns(true)

      val req = request.withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )

      val result = controller.showPage(completionUrl, alternativeVersion = false)(req)

      status(result) shouldBe OK
      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document = Jsoup.parse(contentAsString(result))

      document.select("h1.heading-xlarge").text() shouldBe messages("personal-details.faded-heading") + " " + messages("personal-details.header")
      document.select("h1.heading-xlarge ~ p").text() shouldBe messages("personal-details.paragraph")

      document.select("form[method=POST]").attr("action") shouldBe routes.PersonalDetailsCollectionController.submitMainDetails(completionUrl).url

      document.select("#error-summary-display .js-error-summary-messages").isEmpty shouldBe true

      val backButton = document.select("#identifiersBackLink")
      backButton.text() shouldBe messages("button.back.text")
      backButton.attr("href") shouldBe "javascript:history.back()"

      val fieldsets = document.select("form .form-group")
      val firstNameFieldset = fieldsets.first()
      firstNameFieldset.select("label[for=firstname]").text() shouldBe messages("personal-details.firstname")
      firstNameFieldset.select("label[for=firstname] input[type=text][name=firstName]").isEmpty shouldBe false
      firstNameFieldset.select("input").first().attr("value") shouldBe "Jim"

      val lastNameFieldset = fieldsets.next()
      lastNameFieldset.select("label[for=lastname]").text() shouldBe messages("personal-details.lastname")
      lastNameFieldset.select("label[for=lastname] input[type=text][name=lastName]").isEmpty shouldBe false
      lastNameFieldset.select("input").first().attr("value") shouldBe "Ferguson"

      val dateFieldset = fieldsets.next().select("fieldset")
      dateFieldset.select(".form-label-bold").text() shouldBe messages("personal-details.dateOfBirth")
      dateFieldset.select(".form-hint").text() shouldBe messages("personal-details.dateOfBirth.hint")
      val dateElementDivs = dateFieldset.select(".form-date .form-group")
      val dayElement = dateElementDivs.first()
      dayElement.select("label[for=dateOfBirth.day] span").text() shouldBe messages("personal-details.dateOfBirth.day")
      dayElement.select("label[for=dateOfBirth.day] input[type=text][name=dateOfBirth.day]").isEmpty shouldBe false
      dayElement.select("input[name=dateOfBirth.day]").first().attr("value") shouldBe "1"
      val monthElement = dateElementDivs.next()
      monthElement.select("label[for=dateOfBirth.month] span").text() shouldBe messages("personal-details.dateOfBirth.month")
      monthElement.select("label[for=dateOfBirth.month] input[type=text][name=dateOfBirth.month]").isEmpty shouldBe false
      monthElement.select("input[name=dateOfBirth.month]").first().attr("value") shouldBe "9"
      val yearElement = dateElementDivs.next()
      yearElement.select("label[for=dateOfBirth.year] span").text() shouldBe messages("personal-details.dateOfBirth.year")
      yearElement.select("label[for=dateOfBirth.year] input[type=text][name=dateOfBirth.year]").isEmpty shouldBe false
      yearElement.select("input[name=dateOfBirth.year]").first().attr("value") shouldBe "1939"

      document.select("button[type=submit]").text() shouldBe messages("continue.button.text")
    }
  }

  "submitMainDetails" should {
    "return PersonalDetails when data provided on the form is valid" in new Setup {
      val expectedUrl = routes.PersonalDetailsCollectionController.showNinoForm(completionUrl).url
      val req = request.withFormUrlEncodedBody(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dateOfBirth.day" -> "01",
        "dateOfBirth.month" -> "09",
        "dateOfBirth.year" -> "1939"
      ).withSession("journeyId" -> "1234567890")

      val result = controller.submitMainDetails(completionUrl)(req)

      status(result) shouldBe SEE_OTHER
      redirectLocation(await(result)(5 seconds)).get shouldBe expectedUrl

      val returnedSession = session(result)

      returnedSession.get("firstName") shouldBe defined
      returnedSession.get("firstName").get shouldBe "Jim"

      returnedSession.get("lastName") shouldBe defined
      returnedSession.get("lastName").get shouldBe "Ferguson"

      returnedSession.get("dob") shouldBe defined
      returnedSession.get("dob").get shouldBe "1939-09-01"

      returnedSession.get("journeyId") shouldBe defined
      returnedSession.get("journeyId").get shouldBe "1234567890"
    }

    "display error field validation error when firstname data is missing" in new Setup with BindFromRequestTooling {

      val req = request.withFormUrlEncodedBody(
        "lastName" -> "Ferguson",
        "dateOfBirth.day" -> "01",
        "dateOfBirth.month" -> "09",
        "dateOfBirth.year" -> "1939"
      ).withSession("journeyId" -> "1234567890")

      val result = controller.submitMainDetails(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.firstname.required")

      document.errorFor("firstName") shouldBe messages("personal-details.firstname.required")
    }

    "display error field validation error when lastname data is missing" in new Setup with BindFromRequestTooling {

      val req = request.withFormUrlEncodedBody(
        "firstName" -> "Jim",
        "dateOfBirth.day" -> "01",
        "dateOfBirth.month" -> "09",
        "dateOfBirth.year" -> "1939"
      ).withSession("journeyId" -> "1234567890")

      val result = controller.submitMainDetails(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.lastname.required")

      document.errorFor("lastName") shouldBe messages("personal-details.lastname.required")
    }

    "display error field validation error when day data is missing" in new Setup with BindFromRequestTooling {

      val req = request.withFormUrlEncodedBody(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dateOfBirth.month" -> "09",
        "dateOfBirth.year" -> "1939"
      ).withSession("journeyId" -> "1234567890")

      val result = controller.submitMainDetails(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.dateOfBirth.day.required")

      document.dateError shouldBe messages("personal-details.dateOfBirth.day.required")
    }

    "display error field validation error when month data is missing" in new Setup with BindFromRequestTooling {

      val req = request.withFormUrlEncodedBody(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dateOfBirth.day" -> "01",
        "dateOfBirth.year" -> "1939"
      ).withSession("journeyId" -> "1234567890")

      val result = controller.submitMainDetails(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.dateOfBirth.month.required")

      document.dateError shouldBe messages("personal-details.dateOfBirth.month.required")
    }

    "display error field validation error when year data is missing" in new Setup with BindFromRequestTooling {

      val req = request.withFormUrlEncodedBody(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dateOfBirth.day" -> "01",
        "dateOfBirth.month" -> "09"
      ).withSession("journeyId" -> "1234567890")

      val result = controller.submitMainDetails(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.dateOfBirth.year.required")

      document.dateError shouldBe messages("personal-details.dateOfBirth.year.required")
    }

    "display error field validation error when all dob data is missing" in new Setup with BindFromRequestTooling {

      val req = request.withFormUrlEncodedBody(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson"
      ).withSession("journeyId" -> "1234567890")

      val result = controller.submitMainDetails(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.dateOfBirth.required")

      document.dateError shouldBe messages("personal-details.dateOfBirth.required")
    }

    "display error field validation error when day data is invalid" in new Setup with BindFromRequestTooling {

      val req = request.withFormUrlEncodedBody(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dateOfBirth.day" -> "aaa",
        "dateOfBirth.month" -> "09",
        "dateOfBirth.year" -> "1939"
      ).withSession("journeyId" -> "1234567890")

      val result = controller.submitMainDetails(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.dateOfBirth.day.invalid")

      document.dateError shouldBe messages("personal-details.dateOfBirth.day.invalid")
    }

    "display error field validation error when month data is invalid" in new Setup with BindFromRequestTooling {

      val req = request.withFormUrlEncodedBody(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dateOfBirth.day" -> "01",
        "dateOfBirth.month" -> "aaa",
        "dateOfBirth.year" -> "1939"
      ).withSession("journeyId" -> "1234567890")

      val result = controller.submitMainDetails(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.dateOfBirth.month.invalid")

      document.dateError shouldBe messages("personal-details.dateOfBirth.month.invalid")
    }

    "display error field validation error when year data is invalid" in new Setup with BindFromRequestTooling {

      val req = request.withFormUrlEncodedBody(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dateOfBirth.day" -> "01",
        "dateOfBirth.month" -> "09",
        "dateOfBirth.year" -> "aaa"
      ).withSession("journeyId" -> "1234567890")

      val result = controller.submitMainDetails(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.dateOfBirth.year.invalid")

      document.dateError shouldBe messages("personal-details.dateOfBirth.year.invalid")
    }
  }

  "showNinoForm" should {
    "return OK with the ability to enter the Nino if postcode feature is enabled" in new Setup {
      (mockAppConfig.isMultiPageEnabled _).expects().returns(true)

      val req = request.withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )

      val result = controller.showNinoForm(completionUrl)(req)

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document = Jsoup.parse(contentAsString(result))

      document.select("h1.heading-xlarge").text() shouldBe messages("personal-details.faded-heading") + " " + messages("personal-details.nino.required")

      document.select("form[method=POST]").attr("action") shouldBe routes.PersonalDetailsCollectionController.submitNino(completionUrl).url

      document.select("#error-summary-display .js-error-summary-messages").isEmpty shouldBe true

      val backButton = document.select("#identifiersBackLink")
      backButton.text() shouldBe messages("button.back.text")
      backButton.attr("href") shouldBe "javascript:history.back()"

      val fieldsets = document.select("form .form-group")

      val ninoFieldset = fieldsets.first()
      ninoFieldset.select("label[for=nino] .form-label-bold").text() shouldBe messages("personal-details.nino")
      val ninoHints = ninoFieldset.select("label[for=nino] .form-hint")
      ninoHints.first().text() contains messages("personal-details.nino.hint")
      ninoFieldset.select("label[for=nino] input[type=text][name=nino]").isEmpty shouldBe false

      val otherDetailsLink = ninoFieldset.select("span a").first().attr("href")

      otherDetailsLink shouldBe routes.PersonalDetailsCollectionController.showPostCodeForm(completionUrl).url

      document.select("button[type=submit]").text() shouldBe messages("continue.button.text")
    }

    "Redirect to main if not displaying multi pages" in new Setup {
      val expectedUrl = routes.PersonalDetailsCollectionController.showPage(completionUrl, false).url

      (mockAppConfig.isMultiPageEnabled _).expects().returns(false)

      val req = request.withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )

      val result = controller.showNinoForm(completionUrl)(req)

      status(result) shouldBe SEE_OTHER
      redirectLocation(await(result)(5 seconds)).get shouldBe expectedUrl
    }

    "redirect to the initial page if the initial details are not present in the request" in new Setup {
      val expectedUrl = routes.PersonalDetailsCollectionController.showPage(completionUrl, false).url
      val result = controller.showNinoForm(completionUrl)(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(await(result)(5 seconds)).get shouldBe expectedUrl
    }
  }

  "showPostCodeForm" should {
    "return OK with the ability to enter the Post Code" in new Setup {
      (mockAppConfig.isMultiPageEnabled _).expects().returns(true)

      val req = request.withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )
      val result = controller.showPostCodeForm(completionUrl)(req)

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document = Jsoup.parse(contentAsString(result))

      document.select("h1.heading-xlarge").text() shouldBe messages("personal-details.faded-heading") + " " + messages("personal-details.header.postcode")

      document.select("form[method=POST]").attr("action") shouldBe routes.PersonalDetailsCollectionController.submitPostcode(completionUrl).url

      document.select("#error-summary-display .js-error-summary-messages").isEmpty shouldBe true

      val backButton = document.select("#identifiersBackLink")
      backButton.text() shouldBe messages("button.back.text")
      backButton.attr("href") shouldBe "javascript:history.back()"

      val fieldsets = document.select("form .form-group")

      val postcodeFieldset = fieldsets.first()
      postcodeFieldset.select("label[for=postcode] .form-label-bold").text() shouldBe ""

      document.select("button[type=submit]").text() shouldBe messages("continue.button.text")
    }

    "Redirect to main if not displaying multi pages" in new Setup {
      val expectedUrl = routes.PersonalDetailsCollectionController.showPage(completionUrl, false).url

      (mockAppConfig.isMultiPageEnabled _).expects().returns(false)

      val req = request.withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )
      val result = controller.showPostCodeForm(completionUrl)(req)

      status(result) shouldBe SEE_OTHER
      redirectLocation(await(result)(5 seconds)).get shouldBe expectedUrl
    }

    "redirect to the initial page if the initial details are not present in the request" in new Setup {
      val expectedUrl = routes.PersonalDetailsCollectionController.showPage(completionUrl, false).url
      val result = controller.showPostCodeForm(completionUrl)(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(await(result)(5 seconds)).get shouldBe expectedUrl
    }
  }

  "submitNino" should {
    "succeed when given a valid Nino" in new Setup {
      (mockAppConfig.isMultiPageEnabled _).expects().returns(true)
      val validationId = ValidationId(UUID.randomUUID().toString)
      val expectedRedirect = Redirect(completionUrl.value, Map("validationId" -> Seq(validationId.value)))

      val pdv : EitherT[Future, Result, PersonalDetailsValidation] = EitherT.rightT[Future, Result](new SuccessfulPersonalDetailsValidation(validationId))

      val expectedPersonalDetails = PersonalDetailsWithNino(
        NonEmptyString("Jim"),
        NonEmptyString("Ferguson"),
        Nino("AA000001A"),
        LocalDate.parse("1939-09-01")
      )

      val req = request.withFormUrlEncodedBody("nino" -> "AA000001A").withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )

      (personalDetailsSubmitterMock.submitPersonalDetails(_ : PersonalDetails, _ : CompletionUrl, _ : Boolean)(_: Request[_], _: HeaderCarrier, _: ExecutionContext))
        .expects(expectedPersonalDetails, completionUrl, false, req, instanceOf[HeaderCarrier], instanceOf[ExecutionContext])
        .returns(pdv)

      (personalDetailsSubmitterMock.result(_ : CompletionUrl, _ : PersonalDetailsValidation, _ : Boolean)(_: Request[_]))
        .expects(*, *, *, *)
        .returns(expectedRedirect)

      val result = Await.result(controller.submitNino(completionUrl)(req), 5 seconds)

      result shouldBe expectedRedirect
    }

    "Bad Request if not displaying multi pages" in new Setup {
      (mockAppConfig.isMultiPageEnabled _).expects().returns(false)

      val req = request.withFormUrlEncodedBody("nino" -> "AA000001A").withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )

      val result = Await.result(controller.submitNino(completionUrl)(req), 5 seconds)

      status(result) shouldBe BAD_REQUEST
    }

    "display error field validation error nino missing" in new Setup with BindFromRequestTooling {
      (mockAppConfig.isMultiPageEnabled _).expects().returns(true)

      val req = request.withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )

      val result = controller.submitNino(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.nino.required")

      document.errorFor("nino") shouldBe messages("personal-details.nino.required")
    }

    "display error field validation error nino invalid" in new Setup with BindFromRequestTooling {
      (mockAppConfig.isMultiPageEnabled _).expects().returns(true)

      val req = request.withFormUrlEncodedBody("nino" -> "INVALID") .withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )

      val result = controller.submitNino(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.nino.invalid")

      document.errorFor("nino") shouldBe messages("personal-details.nino.invalid")
    }

    "redirect to showPage if no details found" in new Setup {
      (mockAppConfig.isMultiPageEnabled _).expects().returns(true)
      val validationId = ValidationId(UUID.randomUUID().toString)

      val pdv : EitherT[Future, Result, PersonalDetailsValidation] = EitherT.rightT[Future, Result](FailedPersonalDetailsValidation(validationId))

      val expectedPersonalDetails = PersonalDetailsWithNino(
        NonEmptyString("Jim"),
        NonEmptyString("Ferguson"),
        Nino("AA000001A"),
        LocalDate.parse("1939-09-01")
      )

      val req = request.withFormUrlEncodedBody("nino" -> "AA000001A").withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01",
        "journeyId" -> "1234567890"
      )

      (personalDetailsSubmitterMock.submitPersonalDetails(_ : PersonalDetails, _ : CompletionUrl, _ : Boolean)(_: Request[_], _: HeaderCarrier, _: ExecutionContext))
        .expects(expectedPersonalDetails, completionUrl, false, req, instanceOf[HeaderCarrier], instanceOf[ExecutionContext])
        .returns(pdv)

      val result = controller.submitNino(completionUrl)(req)

      status(result) shouldBe OK
      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document = Jsoup.parse(contentAsString(result))

      document.select("h1.heading-xlarge").text() shouldBe messages("personal-details.faded-heading") + " " + messages("personal-details.header")
      document.select("h1.heading-xlarge ~ p").text() shouldBe messages("personal-details.paragraph")

      document.select("form[method=POST]").attr("action") shouldBe routes.PersonalDetailsCollectionController.submitMainDetails(completionUrl).url

      document.select("#error-summary-display #error-summary-heading").text() shouldBe messages("validation.error-summary.heading")
      document.select("#error-summary-display .js-error-summary-messages").text() shouldBe
        messages("validation.error-summary.before-link-text") +
        " " +
        messages("validation.error-summary.link-text") +
        " " +
        messages("validation.error-summary.after-link-text")

      val fieldsets = document.select("form .form-group")
      val firstNameFieldset = fieldsets.first()
      firstNameFieldset.select("input").first().attr("value") shouldBe ""

      val lastNameFieldset = fieldsets.next()
      lastNameFieldset.select("input").first().attr("value") shouldBe ""

      val dateFieldset = fieldsets.next().select("fieldset")
      val dateElementDivs = dateFieldset.select(".form-date .form-group")
      val dayElement = dateElementDivs.first()
      dayElement.select("input[name=dateOfBirth.day]").first().attr("value") shouldBe ""
      val monthElement = dateElementDivs.next()
      monthElement.select("input[name=dateOfBirth.month]").first().attr("value") shouldBe ""
      val yearElement = dateElementDivs.next()
      yearElement.select("input[name=dateOfBirth.year]").first().attr("value") shouldBe ""

      val returnedSession = session(result)

      returnedSession.get("firstName") shouldBe empty
      returnedSession.get("lastName") shouldBe empty
      returnedSession.get("dob") shouldBe empty
      returnedSession.get("journeyId") shouldBe defined
      returnedSession.get("journeyId").get shouldBe "1234567890"
    }

    "Bad Request if the initial details are not present in the request" in new Setup {
      (mockAppConfig.isMultiPageEnabled _).expects().returns(true)

      val result = controller.submitNino(completionUrl)(request.withFormUrlEncodedBody("nino" -> "AA000001A"))

      status(result) shouldBe BAD_REQUEST
    }
  }

  "submitPostcode" should {
    "succeed when given a valid Nino" in new Setup {
      (mockAppConfig.isMultiPageEnabled _).expects().returns(true)

      val validationId = ValidationId(UUID.randomUUID().toString)
      val expectedRedirect = Redirect(completionUrl.value, Map("validationId" -> Seq(validationId.value)))

      val pdv : EitherT[Future, Result, PersonalDetailsValidation] = EitherT.rightT[Future, Result](new SuccessfulPersonalDetailsValidation(validationId))

      val expectedPersonalDetails = PersonalDetailsWithPostcode(
        NonEmptyString("Jim"),
        NonEmptyString("Ferguson"),
        NonEmptyString("BN1 1NB"),
        LocalDate.parse("1939-09-01")
      )

      val req = request.withFormUrlEncodedBody("postcode" -> "BN1 1NB").withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )

      (personalDetailsSubmitterMock.submitPersonalDetails(_ : PersonalDetails, _ : CompletionUrl, _ : Boolean)(_: Request[_], _: HeaderCarrier, _: ExecutionContext))
        .expects(expectedPersonalDetails, completionUrl, false, req, instanceOf[HeaderCarrier], instanceOf[ExecutionContext])
        .returns(pdv)

      (personalDetailsSubmitterMock.result(_ : CompletionUrl, _ : PersonalDetailsValidation, _ : Boolean)(_: Request[_]))
        .expects(*, *, *, *)
        .returns(expectedRedirect)

      val result = Await.result(controller.submitPostcode(completionUrl)(req), 5 seconds)

      result shouldBe expectedRedirect
    }

    "Bad Request if not displaying multi pages" in new Setup {
      (mockAppConfig.isMultiPageEnabled _).expects().returns(false)

      val req = request.withFormUrlEncodedBody("postcode" -> "BN1 1NB").withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )

      val result = Await.result(controller.submitPostcode(completionUrl)(req), 5 seconds)

      status(result) shouldBe BAD_REQUEST
    }

    "display error field validation error nino missing" in new Setup with BindFromRequestTooling {
      (mockAppConfig.isMultiPageEnabled _).expects().returns(true)

      val req = request.withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )

      val result = controller.submitPostcode(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.postcode.invalid")

      document.errorFor("postcode") shouldBe messages("personal-details.postcode.invalid")
    }

    "display error field validation error nino invalid" in new Setup with BindFromRequestTooling {
      (mockAppConfig.isMultiPageEnabled _).expects().returns(true)

      val req = request.withFormUrlEncodedBody("postcode" -> "INVALID") .withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )

      val result = controller.submitPostcode(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.postcode.invalid")

      document.errorFor("postcode") shouldBe messages("personal-details.postcode.invalid")
    }

    "Bad Request if the initial details are not present in the request" in new Setup {
      (mockAppConfig.isMultiPageEnabled _).expects().returns(true)

      val result = controller.submitPostcode(completionUrl)(request.withFormUrlEncodedBody("postcode" -> "BN11 1NN"))

      status(result) shouldBe BAD_REQUEST
    }

    "redirect to showPage if no details found" in new Setup {
      (mockAppConfig.isMultiPageEnabled _).expects().returns(true)
      val validationId = ValidationId(UUID.randomUUID().toString)

      val pdv : EitherT[Future, Result, PersonalDetailsValidation] = EitherT.rightT[Future, Result](new FailedPersonalDetailsValidation(validationId))

      val expectedPersonalDetails = PersonalDetailsWithPostcode(
        NonEmptyString("Jim"),
        NonEmptyString("Ferguson"),
        NonEmptyString("BN1 1NB"),
        LocalDate.parse("1939-09-01")
      )

      val req = request.withFormUrlEncodedBody("postcode" -> "BN1 1NB").withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01",
        "journeyId" -> "1234567890"
      )

      (personalDetailsSubmitterMock.submitPersonalDetails(_ : PersonalDetails, _ : CompletionUrl, _ : Boolean)(_: Request[_], _: HeaderCarrier, _: ExecutionContext))
        .expects(expectedPersonalDetails, completionUrl, false, req, instanceOf[HeaderCarrier], instanceOf[ExecutionContext])
        .returns(pdv)

      val result = controller.submitPostcode(completionUrl)(req)

      status(result) shouldBe OK
      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document = Jsoup.parse(contentAsString(result))

      document.select("h1.heading-xlarge").text() shouldBe messages("personal-details.faded-heading") + " " + messages("personal-details.header")
      document.select("h1.heading-xlarge ~ p").text() shouldBe messages("personal-details.paragraph")

      document.select("form[method=POST]").attr("action") shouldBe routes.PersonalDetailsCollectionController.submitMainDetails(completionUrl).url

      document.select("#error-summary-display #error-summary-heading").text() shouldBe messages("validation.error-summary.heading")
      document.select("#error-summary-display .js-error-summary-messages").text() shouldBe
        messages("validation.error-summary.before-link-text") +
          " " +
          messages("validation.error-summary.link-text") +
          " " +
          messages("validation.error-summary.after-link-text")

      val fieldsets = document.select("form .form-group")
      val firstNameFieldset = fieldsets.first()
      firstNameFieldset.select("input").first().attr("value") shouldBe ""

      val lastNameFieldset = fieldsets.next()
      lastNameFieldset.select("input").first().attr("value") shouldBe ""

      val dateFieldset = fieldsets.next().select("fieldset")
      val dateElementDivs = dateFieldset.select(".form-date .form-group")
      val dayElement = dateElementDivs.first()
      dayElement.select("input[name=dateOfBirth.day]").first().attr("value") shouldBe ""
      val monthElement = dateElementDivs.next()
      monthElement.select("input[name=dateOfBirth.month]").first().attr("value") shouldBe ""
      val yearElement = dateElementDivs.next()
      yearElement.select("input[name=dateOfBirth.year]").first().attr("value") shouldBe ""

      val returnedSession = session(result)

      returnedSession.get("firstName") shouldBe empty
      returnedSession.get("lastName") shouldBe empty
      returnedSession.get("dob") shouldBe empty
      returnedSession.get("journeyId") shouldBe defined
      returnedSession.get("journeyId").get shouldBe "1234567890"
    }
  }

  "submit" should {

    "pass the outcome of bindValidateAndRedirect" in new Setup {

      val redirectUrl = s"${completionUrl.value}?validationId=${UUID.randomUUID()}"

      (personalDetailsSubmitterMock.submit(_: CompletionUrl, _: Boolean)(_: Request[_], _: HeaderCarrier, _: ExecutionContext))
        .expects(completionUrl, false, request, instanceOf[HeaderCarrier], instanceOf[ExecutionContext])
        .returning(Future.successful(Redirect(redirectUrl)))

      val result = controller.submit(completionUrl, alternativeVersion = false)(request)

      redirectLocation(Await.result(result, 5 seconds)) shouldBe Some(redirectUrl)
    }
  }

  "keep-alive" should {

    "return 200 OK" in new Setup {
      private val result = controller.keepAlive()(request)
      status(result) shouldBe 200
      Mockito.verify(mockEventDispatcher).dispatchEvent(TimeoutContinue)
    }

  }

  "redirect-after-timeout" should {

    "redirect user to continueUrl with userTimeout parameter" in new Setup {

      private val redirectUrl = s"${completionUrl.value}?userTimeout="

      private val result = controller.redirectAfterTimeout(completionUrl)(request)
      status(result) shouldBe 303
      redirectLocation(Await.result(result, 5 seconds)) shouldBe Some(redirectUrl)
      Mockito.verify(mockEventDispatcher).dispatchEvent(TimedOut)
    }

  }

  private trait Setup {

    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

    val completionUrl = ValuesGenerators.completionUrls.generateOne

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    implicit val lang: Lang = Lang("en-GB")
    implicit val messages: Messages = Messages.Implicits.applicationMessages

    val pageMock: PersonalDetailsPage = mock[PersonalDetailsPage]
    val personalDetailsSubmitterMock = mock[FuturedPersonalDetailsSubmission]
    val mockAppConfig = mock[AppConfig]
    val mockEventDispatcher: EventDispatcher = mock[EventDispatcher]
    implicit val mockViewConfig = app.injector.instanceOf[ViewConfig]
    implicit val dwpMessagesApiProvider = app.injector.instanceOf[DwpMessagesApiProvider]

    def stubMessagesControllerComponents() : MessagesControllerComponents = {
      val stub = stubControllerComponents()
      DefaultMessagesControllerComponents(
        new DefaultMessagesActionBuilderImpl(stubBodyParser(AnyContentAsEmpty), dwpMessagesApiProvider.get)(stub.executionContext),
        DefaultActionBuilder(stub.actionBuilder.parser)(stub.executionContext), stub.parsers, dwpMessagesApiProvider.get, stub.langs, stub.fileMimeTypes,
        stub.executionContext
      )
    }

    private val enter_your_details_postcode: enter_your_details_postcode = app.injector.instanceOf[enter_your_details_postcode]

    private val enter_your_details_nino: enter_your_details_nino = app.injector.instanceOf[enter_your_details_nino]

    private val personal_details_main: personal_details_main = app.injector.instanceOf[personal_details_main]

    val controller = new PersonalDetailsCollectionController(
      pageMock,
      personalDetailsSubmitterMock,
      mockAppConfig,
      mockEventDispatcher,
      stubMessagesControllerComponents(),
      enter_your_details_nino,
      enter_your_details_postcode,
      personal_details_main)

    implicit val hc: HeaderCarrier = HeaderCarrier()
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
          page.select("div[class=flash error-summary error-summary--show]")

        lazy val heading = errorsSummaryDiv.select("h2").text()

        lazy val content = errorsSummaryDiv.select("ul").text()
      }

      def errorFor(fieldName: String): String = {
        val control = page.select(s"label[for=$fieldName].form-field--error")
        control.isEmpty shouldBe false
        control.select(".error-notification").text()
      }

      lazy val dateError: String =
        page.select("div[class=form-date]")
          .parents()
          .first()
          .select(".error-notification")
          .text()
    }
  }
}
