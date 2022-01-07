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

package uk.gov.hmrc.personaldetailsvalidation.endpoints

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.data._
import cats.implicits.catsStdInstancesForFuture
import generators.Generators.Implicits._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.mvc.Results._
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import setups.controllers.ResultVerifiers._
import support.UnitSpec
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.config.{AppConfig, DwpMessagesApiProvider}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.personaldetailsvalidation.connectors.IdentityVerificationConnector
import uk.gov.hmrc.personaldetailsvalidation.generators.ObjectGenerators.{personalDetailsObjects, personalDetailsObjectsWithPostcode}
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators
import uk.gov.hmrc.personaldetailsvalidation.model._
import uk.gov.hmrc.personaldetailsvalidation.monitoring._
import uk.gov.hmrc.personaldetailsvalidation.views.html.pages.we_cannot_check_your_identity
import uk.gov.hmrc.personaldetailsvalidation.views.html.template._
import uk.gov.hmrc.personaldetailsvalidation.views.pages.PersonalDetailsPage
import uk.gov.hmrc.views.ViewConfig

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class PersonalDetailsCollectionControllerSpec extends UnitSpec with MockFactory with GuiceOneAppPerSuite {

  "showPage" should {

    //todo: old multi journey, need replaced
    "return OK with HTML body rendered using PersonalDetailsPage when the multi page flag is disabled" in new Setup {
      (mockAppConfig.isMultiPageEnabled _: () => Boolean).expects().returning(false)
      (pageMock.render(_: Boolean, _ : Boolean)(_: CompletionUrl, _: Request[_]))
        .expects(false, false, completionUrl, *)
        .returning(Html("content"))

      val result: Future[Result] = controller.showPage(completionUrl, alternativeVersion = false, None)(request)

      verify(result).has(statusCode = OK, content = "content")
    }

    //todo: old multi journey, need replaced
    "return OK with simplified first page when the multi page flag is enabled" in new Setup {
      (mockAppConfig.isMultiPageEnabled _: () => Boolean).expects().returning(false)
      (pageMock.render(_: Boolean, _ : Boolean)(_: CompletionUrl, _: Request[_]))
        .expects(false, false, completionUrl, *)
        .returning(Html("content"))

      val result: Future[Result] = controller.showPage(completionUrl, alternativeVersion = false, None)(request)

      status(result) shouldBe OK
      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.select("#error-summary-display .js-error-summary-messages").isEmpty shouldBe true

    }

    //todo: old multi journey, need replaced
    "return OK with simplified first page, containing data from session, when the multi page flag is enabled" in new Setup {
      (mockAppConfig.isMultiPageEnabled _: () => Boolean).expects().returning(false)
      (pageMock.render(_: Boolean, _ : Boolean)(_: CompletionUrl, _: Request[_]))
        .expects(false, false, completionUrl, *)
        .returning(Html("content"))

      val req: FakeRequest[AnyContentAsEmpty.type] = request.withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )

      val result: Future[Result] = controller.showPage(completionUrl, alternativeVersion = false, None)(req)

      status(result) shouldBe OK
      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.select("#error-summary-display .js-error-summary-messages").isEmpty shouldBe true

    }
  }

  //todo: old multi journey, need replaced
  "submitMainDetails" should {
    "return PersonalDetails when data provided on the form is valid" in new Setup {
      val expectedUrl: String = routes.PersonalDetailsCollectionController.showNinoForm(completionUrl).url
      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dateOfBirth.day" -> "01",
        "dateOfBirth.month" -> "09",
        "dateOfBirth.year" -> "1939"
      ).withSession("journeyId" -> "1234567890")

      val result: Future[Result] = controller.submitMainDetails(completionUrl)(req)

      status(result) shouldBe SEE_OTHER
      redirectLocation(await(result)(5 seconds)).get shouldBe expectedUrl

      val returnedSession: Session = session(result)

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

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody(
        "lastName" -> "Ferguson",
        "dateOfBirth.day" -> "01",
        "dateOfBirth.month" -> "09",
        "dateOfBirth.year" -> "1939"
      ).withSession("journeyId" -> "1234567890")

      val result: Future[Result] = controller.submitMainDetails(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("validation.error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.firstname.required")
    }

    "display error field validation error when lastname data is missing" in new Setup with BindFromRequestTooling {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody(
        "firstName" -> "Jim",
        "dateOfBirth.day" -> "01",
        "dateOfBirth.month" -> "09",
        "dateOfBirth.year" -> "1939"
      ).withSession("journeyId" -> "1234567890")

      val result: Future[Result] = controller.submitMainDetails(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("validation.error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.lastname.required")
    }

    "display error field validation error when day data is missing" in new Setup with BindFromRequestTooling {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dateOfBirth.month" -> "09",
        "dateOfBirth.year" -> "1939"
      ).withSession("journeyId" -> "1234567890")

      val result: Future[Result] = controller.submitMainDetails(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("validation.error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.dateOfBirth.day.required")

      document.dateError shouldBe "Error: "+messages("personal-details.dateOfBirth.day.required")
    }

    "display error field validation error when month data is missing" in new Setup with BindFromRequestTooling {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dateOfBirth.day" -> "01",
        "dateOfBirth.year" -> "1939"
      ).withSession("journeyId" -> "1234567890")

      val result: Future[Result] = controller.submitMainDetails(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("validation.error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.dateOfBirth.month.required")

      document.dateError shouldBe "Error: "+messages("personal-details.dateOfBirth.month.required")
    }

    "display error field validation error when year data is missing" in new Setup with BindFromRequestTooling {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dateOfBirth.day" -> "01",
        "dateOfBirth.month" -> "09"
      ).withSession("journeyId" -> "1234567890")

      val result: Future[Result] = controller.submitMainDetails(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("validation.error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.dateOfBirth.year.required")

      document.dateError shouldBe "Error: "+messages("personal-details.dateOfBirth.year.required")
    }

    "display error field validation error when all dob data is missing" in new Setup with BindFromRequestTooling {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson"
      ).withSession("journeyId" -> "1234567890")

      val result: Future[Result] = controller.submitMainDetails(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("validation.error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.dateOfBirth.required")

      document.dateError shouldBe "Error: "+messages("personal-details.dateOfBirth.required")
    }

    "display error field validation error when day data is invalid" in new Setup with BindFromRequestTooling {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dateOfBirth.day" -> "aaa",
        "dateOfBirth.month" -> "09",
        "dateOfBirth.year" -> "1939"
      ).withSession("journeyId" -> "1234567890")

      val result: Future[Result] = controller.submitMainDetails(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("validation.error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.dateOfBirth.day.invalid")

      document.dateError shouldBe "Error: "+messages("personal-details.dateOfBirth.day.invalid")
    }

    "display error field validation error when month data is invalid" in new Setup with BindFromRequestTooling {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dateOfBirth.day" -> "01",
        "dateOfBirth.month" -> "aaa",
        "dateOfBirth.year" -> "1939"
      ).withSession("journeyId" -> "1234567890")

      val result: Future[Result] = controller.submitMainDetails(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("validation.error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.dateOfBirth.month.invalid")

      document.dateError shouldBe "Error: "+messages("personal-details.dateOfBirth.month.invalid")
    }

    "display error field validation error when year data is invalid" in new Setup with BindFromRequestTooling {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dateOfBirth.day" -> "01",
        "dateOfBirth.month" -> "09",
        "dateOfBirth.year" -> "aaa"
      ).withSession("journeyId" -> "1234567890")

      val result: Future[Result] = controller.submitMainDetails(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("validation.error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.dateOfBirth.year.invalid")

      document.dateError shouldBe "Error: "+messages("personal-details.dateOfBirth.year.invalid")
    }
  }

  //todo: old multi journey, need replaced
  "showNinoForm" should {
    "return OK with the ability to enter the Nino if postcode feature is enabled" in new Setup {
      (mockAppConfig.isMultiPageEnabled _: () => Boolean).expects().returning(false)
      val req: FakeRequest[AnyContentAsEmpty.type] = request.withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )

      val result: Future[Result] = controller.showNinoForm(completionUrl)(req)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("#error-summary-display .js-error-summary-messages").isEmpty shouldBe true
    }

    "Redirect to main if not displaying multi pages" in new Setup {
      (mockAppConfig.isMultiPageEnabled _: () => Boolean).expects().returning(false)
      val expectedUrl: String = routes.PersonalDetailsCollectionController.showPage(completionUrl, false, None).url

      val req: FakeRequest[AnyContentAsEmpty.type] = request.withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )

      val result: Future[Result] = controller.showNinoForm(completionUrl)(req)

      status(result) shouldBe SEE_OTHER
      redirectLocation(await(result)(5 seconds)).get shouldBe expectedUrl
    }

    "redirect to the initial page if the initial details are not present in the request" in new Setup {
      val expectedUrl: String = routes.PersonalDetailsCollectionController.showPage(completionUrl, false, None).url
      val result: Future[Result] = controller.showNinoForm(completionUrl)(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(await(result)(5 seconds)).get shouldBe expectedUrl
    }
  }

  "Postcode Regex validation should work as expected" should{

    "validate valid postcodes" in new Setup {
      controller.postcodeFormatValidation(NonEmptyString("BN12 4XH")) shouldBe true
      controller.postcodeFormatValidation(NonEmptyString("bn12 4xh")) shouldBe true //lowercase also ok
      controller.postcodeFormatValidation(NonEmptyString("L13 1xy")) shouldBe true
      controller.postcodeFormatValidation(NonEmptyString("J1 2FE")) shouldBe true
    }

    "not validate invalid postcodes that will fail on the address lookup service" in new Setup{
      controller.postcodeFormatValidation(NonEmptyString("BN12   4XH")) shouldBe false //can't have more than 1 space
      // according to existing code
      controller.postcodeFormatValidation(NonEmptyString("J1 22FE")) shouldBe false //can't have 2 numbers in 2nd part
      controller.postcodeFormatValidation(NonEmptyString("CR 2JJ")) shouldBe false //first part doesn't end with number
      controller.postcodeFormatValidation(NonEmptyString("J1 2F")) shouldBe false //2nd part doesn't end in 2 letters
    }
 }

  //todo: old multi journey, need replaced
  "showPostCodeForm" should {
    "return OK with the ability to enter the Post Code" in new Setup {
      (mockAppConfig.isMultiPageEnabled _: () => Boolean).expects().returning(false)
      val req: FakeRequest[AnyContentAsEmpty.type] = request.withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )
      val result: Future[Result] = controller.showPostCodeForm(completionUrl)(req)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("#error-summary-display .js-error-summary-messages").isEmpty shouldBe true

    }

    "Redirect to main if not displaying multi pages" in new Setup {
      (mockAppConfig.isMultiPageEnabled _: () => Boolean).expects().returning(false)
      val expectedUrl: String = routes.PersonalDetailsCollectionController.showPage(completionUrl, false, None).url

      val req: FakeRequest[AnyContentAsEmpty.type] = request.withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )
      val result: Future[Result] = controller.showPostCodeForm(completionUrl)(req)

      status(result) shouldBe SEE_OTHER
      redirectLocation(await(result)(5 seconds)).get shouldBe expectedUrl
    }

    "redirect to the initial page if the initial details are not present in the request" in new Setup {
      val expectedUrl: String = routes.PersonalDetailsCollectionController.showPage(completionUrl, false, None).url
      val result: Future[Result] = controller.showPostCodeForm(completionUrl)(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(await(result)(5 seconds)).get shouldBe expectedUrl
    }
  }

  //todo: old multi journey, need replaced
  "submitNino" should {
    "succeed when given a valid Nino" in new Setup {
      (mockAppConfig.isMultiPageEnabled _: () => Boolean).expects().returning(true)
      (mockViewConfig.isLoggedIn(_: HeaderCarrier, _: ExecutionContext)).expects(*, *).returning(Future.successful(true))
      val validationId: ValidationId = ValidationId(UUID.randomUUID().toString)
      val expectedRedirect: Result = Redirect(completionUrl.value, Map("validationId" -> Seq(validationId.value)))

      val pdv : EitherT[Future, Result, PersonalDetailsValidation] = EitherT.rightT[Future, Result](new SuccessfulPersonalDetailsValidation(validationId))

      val expectedPersonalDetails: PersonalDetailsWithNino = PersonalDetailsWithNino(
        NonEmptyString("Jim"),
        NonEmptyString("Ferguson"),
        Nino("AA000001A"),
        LocalDate.parse("1939-09-01")
      )

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody("nino" -> "AA000001A").withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )

      (personalDetailsSubmitterMock.submitPersonalDetails(_ : PersonalDetails, _ : CompletionUrl, _ : Boolean, _ : Boolean)(_: Request[_], _: HeaderCarrier, _: ExecutionContext))
        .expects(expectedPersonalDetails, completionUrl, false, true, *, *, *)
        .returns(pdv)

      (personalDetailsSubmitterMock.result(_ : CompletionUrl, _ : PersonalDetailsValidation, _ : Boolean, _ : Boolean)(_: Request[_]))
        .expects(*, *, *, *, *)
        .returns(expectedRedirect)

      val result: Result = Await.result(controllerWithMockViewConfig.submitNino(completionUrl)(req), 5 seconds)

      result shouldBe expectedRedirect
    }

    "Bad Request if not displaying multi pages" in new Setup {
      (mockAppConfig.isMultiPageEnabled _: () => Boolean).expects().returning(false)

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody("nino" -> "AA000001A").withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )

      val result: Result = Await.result(controller.submitNino(completionUrl)(req), 5 seconds)

      status(result) shouldBe BAD_REQUEST
    }

    "display error field validation error nino missing" in new Setup with BindFromRequestTooling {
      (mockAppConfig.isMultiPageEnabled _: () => Boolean).expects().returning(true)
      val req: FakeRequest[AnyContentAsEmpty.type] = request.withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )

      val result: Future[Result] = controller.submitNino(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("validation.error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.nino.required")

    }

    "display error field validation error nino invalid" in new Setup with BindFromRequestTooling {
      (mockAppConfig.isMultiPageEnabled _: () => Boolean).expects().returning(true)
      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody("nino" -> "INVALID") .withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )

      val result: Future[Result] = controller.submitNino(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("validation.error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.nino.invalid")

    }

    "redirect to showPage if no details found" in new Setup {
      (mockAppConfig.isMultiPageEnabled _: () => Boolean).expects().returning(true)
      val validationId: ValidationId = ValidationId(UUID.randomUUID().toString)

      val pdv : EitherT[Future, Result, PersonalDetailsValidation] = EitherT.rightT[Future, Result](FailedPersonalDetailsValidation(validationId))

      val expectedPersonalDetails: PersonalDetailsWithNino = PersonalDetailsWithNino(
        NonEmptyString("Jim"),
        NonEmptyString("Ferguson"),
        Nino("AA000001A"),
        LocalDate.parse("1939-09-01")
      )

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody("nino" -> "AA000001A").withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01",
        "journeyId" -> "1234567890"
      )

      (personalDetailsSubmitterMock.submitPersonalDetails(_ : PersonalDetails, _ : CompletionUrl, _ : Boolean, _ : Boolean)(_: Request[_], _: HeaderCarrier, _: ExecutionContext))
        .expects(expectedPersonalDetails, completionUrl, false, *, *, *, *)
        .returns(pdv)

      val result: Future[Result] = controller.submitNino(completionUrl)(req)

      status(result) shouldBe SEE_OTHER

    }

    "Bad Request if the initial details are not present in the request" in new Setup {
      (mockAppConfig.isMultiPageEnabled _: () => Boolean).expects().returning(true)
      val result: Future[Result] = controller.submitNino(completionUrl)(request.withFormUrlEncodedBody("nino" -> "AA000001A"))

      status(result) shouldBe BAD_REQUEST
    }
  }

  //todo: old multi journey, need replaced
  "submitPostcode" should {
    "succeed when given a valid Nino" in new Setup {
      (mockAppConfig.isMultiPageEnabled _: () => Boolean).expects().returning(true)
      (mockViewConfig.isLoggedIn(_: HeaderCarrier, _: ExecutionContext)).expects(*, *).returning(Future.successful(true))
      val validationId: ValidationId = ValidationId(UUID.randomUUID().toString)
      val expectedRedirect: Result = Redirect(completionUrl.value, Map("validationId" -> Seq(validationId.value)))

      val pdv : EitherT[Future, Result, PersonalDetailsValidation] = EitherT.rightT[Future, Result](new SuccessfulPersonalDetailsValidation(validationId))

      val expectedPersonalDetails: PersonalDetailsWithPostcode = PersonalDetailsWithPostcode(
        NonEmptyString("Jim"),
        NonEmptyString("Ferguson"),
        NonEmptyString("BN1 1NB"),
        LocalDate.parse("1939-09-01")
      )

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody("postcode" -> "BN1 1NB").withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )

      (personalDetailsSubmitterMock.submitPersonalDetails(_ : PersonalDetails, _ : CompletionUrl, _ : Boolean, _ : Boolean)(_: Request[_], _: HeaderCarrier, _: ExecutionContext))
        .expects(expectedPersonalDetails, completionUrl, false, true, *, *, *)
        .returns(pdv)

      (personalDetailsSubmitterMock.result(_ : CompletionUrl, _ : PersonalDetailsValidation, _ : Boolean, _ : Boolean)(_: Request[_]))
        .expects(*, *, *, *, *)
        .returns(expectedRedirect)

      val result: Result = Await.result(controllerWithMockViewConfig.submitPostcode(completionUrl)(req), 5 seconds)

      result shouldBe expectedRedirect
    }

    "Bad Request if not displaying multi pages" in new Setup {
      (mockAppConfig.isMultiPageEnabled _: () => Boolean).expects().returning(false)
      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody("postcode" -> "BN1 1NB").withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )

      val result: Result = Await.result(controllerWithMockViewConfig.submitPostcode(completionUrl)(req), 5 seconds)

      status(result) shouldBe BAD_REQUEST
    }

    "display error field validation error nino missing" in new Setup with BindFromRequestTooling {
      (mockAppConfig.isMultiPageEnabled _: () => Boolean).expects().returning(true)
      val req: FakeRequest[AnyContentAsEmpty.type] = request.withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )

      val result: Future[Result] = controller.submitPostcode(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("validation.error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.postcode.invalid")

    }

    "display error field validation error nino invalid" in new Setup with BindFromRequestTooling {
      (mockAppConfig.isMultiPageEnabled _: () => Boolean).expects().returning(true)
      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody("postcode" -> "INVALID") .withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )

      val result: Future[Result] = controller.submitPostcode(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("validation.error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.postcode.invalid")
    }

    "Bad Request if the initial details are not present in the request" in new Setup {
      (mockAppConfig.isMultiPageEnabled _: () => Boolean).expects().returning(true)
      val result: Future[Result] = controller.submitPostcode(completionUrl)(request.withFormUrlEncodedBody("postcode" -> "BN11 1NN"))

      status(result) shouldBe BAD_REQUEST
    }

    "redirect to showPage if no details found" in new Setup {
      (mockAppConfig.isMultiPageEnabled _: () => Boolean).expects().returning(true)
      val validationId: ValidationId = ValidationId(UUID.randomUUID().toString)

      val pdv : EitherT[Future, Result, PersonalDetailsValidation] = EitherT.rightT[Future, Result](new FailedPersonalDetailsValidation(validationId))

      val expectedPersonalDetails: PersonalDetailsWithPostcode = PersonalDetailsWithPostcode(
        NonEmptyString("Jim"),
        NonEmptyString("Ferguson"),
        NonEmptyString("BN1 1NB"),
        LocalDate.parse("1939-09-01")
      )

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody("postcode" -> "BN1 1NB").withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01",
        "journeyId" -> "1234567890"
      )

      (personalDetailsSubmitterMock.submitPersonalDetails(_ : PersonalDetails, _ : CompletionUrl, _ : Boolean, _ : Boolean)(_: Request[_], _: HeaderCarrier, _: ExecutionContext))
        .expects(expectedPersonalDetails, completionUrl, false, false, *, *, *)
        .returns(pdv)

      val result: Future[Result] = controller.submitPostcode(completionUrl)(req)

      status(result) shouldBe SEE_OTHER
    }
  }

  "submit" should {

    "pass the outcome of bindValidateAndRedirect" in new Setup {
      (mockViewConfig.isLoggedIn(_: HeaderCarrier, _: ExecutionContext)).expects(*, *).returning(Future.successful(true))
      val redirectUrl = s"${completionUrl.value}?validationId=${UUID.randomUUID()}"

      (personalDetailsSubmitterMock.submit(_: CompletionUrl, _: Boolean, _: Boolean)(_: Request[_], _: HeaderCarrier, _: ExecutionContext))
        .expects(completionUrl, false, *, *, *, *)
        .returning(Future.successful(Redirect(redirectUrl)))

      val result: Future[Result] = controllerWithMockViewConfig.submit(completionUrl, alternativeVersion = false)(request)

      redirectLocation(Await.result(result, 5 seconds)) shouldBe Some(redirectUrl)
    }
  }

  "keep-alive" should {

    "return 200 OK" in new Setup {
      (mockEventDispatcher.dispatchEvent(_: MonitoringEvent)(_: Request[_], _: HeaderCarrier, _: ExecutionContext)).expects(TimeoutContinue, *, *, *)
      val result: Future[Result] = controller.keepAlive()(request)
      status(result) shouldBe 200
    }

  }

  "we-cannot-check-your-identity" should {

    "return 200 OK" in new Setup {
      (mockEventDispatcher.dispatchEvent(_: MonitoringEvent)(_: Request[_], _: HeaderCarrier, _: ExecutionContext)).expects(UnderNinoAge, *, *, *)

      val result: Future[Result] = controller.weCannotCheckYourIdentity()(request)

      status(result) shouldBe 200
      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.select("h1").text() shouldBe messages("we-cannot-check-your-identity.header")
    }

  }

  "redirect-after-timeout" should {

    "redirect user to continueUrl with userTimeout parameter" in new Setup {

      val redirectUrl = s"${completionUrl.value}&userTimeout="

      (mockEventDispatcher.dispatchEvent(_: MonitoringEvent)(_: Request[_], _: HeaderCarrier, _: ExecutionContext)).expects(TimedOut, *, *, *)
      (mockIVConnector.updateJourney(_: String)(_: HeaderCarrier, _: ExecutionContext)).expects(*, *, *)

      val result = controller.redirectAfterTimeout(completionUrl)(request)
      status(result) shouldBe 303
      redirectLocation(Await.result(result, 5 seconds)).get shouldBe redirectUrl

    }

  }

  private trait Setup {

    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

    val completionUrl: CompletionUrl = ValuesGenerators.completionUrls.generateOne

    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    implicit val dwpMessagesApiProvider: DwpMessagesApiProvider = app.injector.instanceOf[DwpMessagesApiProvider]
    implicit val lang: Lang = Lang("en-GB")
    implicit val messages: Messages = MessagesImpl(lang, dwpMessagesApiProvider.get)
    implicit val ec: ExecutionContext = ExecutionContext.global

    val pageMock: PersonalDetailsPage = mock[PersonalDetailsPage]
    val personalDetailsSubmitterMock: FuturedPersonalDetailsSubmission = mock[FuturedPersonalDetailsSubmission]
    val mockAppConfig: AppConfig = mock[AppConfig]
    val mockEventDispatcher: EventDispatcher = mock[EventDispatcher]
    val mockIVConnector: IdentityVerificationConnector = mock[IdentityVerificationConnector]
    val mockViewConfig: ViewConfig = mock[ViewConfig]
    implicit val viewConfig: ViewConfig = app.injector.instanceOf[ViewConfig]

    def stubMessagesControllerComponents() : MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

    private val enter_your_details_postcode: enter_your_details_postcode = app.injector.instanceOf[enter_your_details_postcode]
    private val what_is_your_postcode: what_is_your_postcode = app.injector.instanceOf[what_is_your_postcode]
    private val enter_your_details: enter_your_details = app.injector.instanceOf[enter_your_details]

    private val enter_your_details_nino: enter_your_details_nino = app.injector.instanceOf[enter_your_details_nino]
    private val what_is_your_nino: what_is_your_nino = app.injector.instanceOf[what_is_your_nino]

    private val we_cannot_check_your_identity: we_cannot_check_your_identity = app.injector.instanceOf[we_cannot_check_your_identity]

    private val personal_details_main: personal_details_main = app.injector.instanceOf[personal_details_main]

    implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

    implicit val authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]

    val controller = new PersonalDetailsCollectionController(
      pageMock,
      personalDetailsSubmitterMock,
      mockAppConfig,
      mockEventDispatcher,
      stubMessagesControllerComponents(),
      enter_your_details_nino,
      enter_your_details_postcode,
      what_is_your_postcode,
      what_is_your_nino,
      enter_your_details,
      personal_details_main,
      we_cannot_check_your_identity,
      mockIVConnector)

    val controllerWithMockViewConfig = new PersonalDetailsCollectionController(
      pageMock,
      personalDetailsSubmitterMock,
      mockAppConfig,
      mockEventDispatcher,
      stubMessagesControllerComponents(),
      enter_your_details_nino,
      enter_your_details_postcode,
      what_is_your_postcode,
      what_is_your_nino,
      enter_your_details,
      personal_details_main,
      we_cannot_check_your_identity,
      mockIVConnector)(authConnector, dwpMessagesApiProvider, mockViewConfig, ec, messagesApi)

    implicit val hc: HeaderCarrier = HeaderCarrier()
  }

  private trait BindFromRequestTooling {

    self: Setup =>

    val personalDetails: PersonalDetailsWithNino = personalDetailsObjects.generateOne

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

    val personalDetailsWithPostcode: PersonalDetailsWithPostcode = personalDetailsObjectsWithPostcode.generateOne

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

      lazy val errorsSummary: Object {
        val heading: String

        val content: String
      } = new {

        private lazy val errorsSummaryDiv =
          page.select("div[class=govuk-error-summary]")

        lazy val heading: String = errorsSummaryDiv.select("h2").text()

        lazy val content: String = errorsSummaryDiv.select("ul").text()
      }

      def errorFor(fieldName: String): String = {
        val control = page.select(s"label[for=$fieldName].form-field--error")
        control.isEmpty shouldBe true
        control.select(s".govuk-form-group--error").text()
      }

      lazy val dateError: String =
        page.select("div[class=govuk-form-group govuk-form-group--error]")
          .parents()
          .first()
          .select(".govuk-error-message")
          .text()
    }
  }
}
