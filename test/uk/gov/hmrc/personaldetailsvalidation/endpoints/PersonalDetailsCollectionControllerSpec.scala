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
import akka.stream.Materializer
import cats.data._
import cats.implicits._
import generators.Generators.Implicits._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.mvc.Results._
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.UnitSpec
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.config.{AppConfig, DwpMessagesApiProvider}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.personaldetailsvalidation.connectors.IdentityVerificationConnector
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators
import uk.gov.hmrc.personaldetailsvalidation.model._
import uk.gov.hmrc.personaldetailsvalidation.monitoring._
import uk.gov.hmrc.personaldetailsvalidation.views.html.pages.we_cannot_check_your_identity
import uk.gov.hmrc.personaldetailsvalidation.views.html.template._
import uk.gov.hmrc.views.ViewConfig

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class PersonalDetailsCollectionControllerSpec extends UnitSpec with MockFactory with GuiceOneAppPerSuite {

  "showPage" should {

    "Redirect to enter-your-details page when user call /personal-details" in new Setup {

      val result: Future[Result] = controller.showPage(completionUrl, None)(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(await(result)).get.contains("/personal-details-validation/enter-your-details?completionUrl=") shouldBe true

    }

    "return enter-your-details page, containing data from session" in new Setup {

      val result: Future[Result] = controller.enterYourDetails(completionUrl)(request)

      status(result) shouldBe OK
      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.select("form[method=POST]").attr("action") shouldBe routes.PersonalDetailsCollectionController.submitYourDetails(completionUrl).url
      document.select("#error-summary-display .js-error-summary-messages").isEmpty shouldBe true
      document.select("button[type=submit]").text() shouldBe messages("continue.button.text")
    }
  }

  "submitMainDetails" should {
    "return PersonalDetails when data provided on the form is valid" in new Setup {
      val expectedUrl: String = routes.PersonalDetailsCollectionController.whatIsYourNino(completionUrl).url
      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dateOfBirth.day" -> "01",
        "dateOfBirth.month" -> "09",
        "dateOfBirth.year" -> "1939"
      ).withSession("journeyId" -> "1234567890")

      val result: Future[Result] = controller.submitYourDetails(completionUrl)(req)

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

      val result: Future[Result] = controller.submitYourDetails(completionUrl)(req)

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

      val result: Future[Result] = controller.submitYourDetails(completionUrl)(req)

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

      val result: Future[Result] = controller.submitYourDetails(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("validation.error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.dateOfBirth.day.required")
    }

    "display error field validation error when month data is missing" in new Setup with BindFromRequestTooling {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dateOfBirth.day" -> "01",
        "dateOfBirth.year" -> "1939"
      ).withSession("journeyId" -> "1234567890")

      val result: Future[Result] = controller.submitYourDetails(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("validation.error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.dateOfBirth.month.required")
    }

    "display error field validation error when year data is missing" in new Setup with BindFromRequestTooling {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dateOfBirth.day" -> "01",
        "dateOfBirth.month" -> "09"
      ).withSession("journeyId" -> "1234567890")

      val result: Future[Result] = controller.submitYourDetails(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("validation.error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.dateOfBirth.year.required")
    }

    "display error field validation error when all dob data is missing" in new Setup with BindFromRequestTooling {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson"
      ).withSession("journeyId" -> "1234567890")

      val result: Future[Result] = controller.submitYourDetails(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("validation.error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.dateOfBirth.required")
    }

    "display error field validation error when day data is invalid" in new Setup with BindFromRequestTooling {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dateOfBirth.day" -> "aaa",
        "dateOfBirth.month" -> "09",
        "dateOfBirth.year" -> "1939"
      ).withSession("journeyId" -> "1234567890")

      val result: Future[Result] = controller.submitYourDetails(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("validation.error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.dateOfBirth.day.invalid")
    }

    "display error field validation error when month data is invalid" in new Setup with BindFromRequestTooling {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dateOfBirth.day" -> "01",
        "dateOfBirth.month" -> "aaa",
        "dateOfBirth.year" -> "1939"
      ).withSession("journeyId" -> "1234567890")

      val result: Future[Result] = controller.submitYourDetails(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("validation.error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.dateOfBirth.month.invalid")

    }

    "display error field validation error when year data is invalid" in new Setup with BindFromRequestTooling {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dateOfBirth.day" -> "01",
        "dateOfBirth.month" -> "09",
        "dateOfBirth.year" -> "aaa"
      ).withSession("journeyId" -> "1234567890")

      val result: Future[Result] = controller.submitYourDetails(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("validation.error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.dateOfBirth.year.invalid")
    }
  }

  "showNinoForm" should {
    "return OK with the ability to enter the Nino" in new Setup {

      val req: FakeRequest[AnyContentAsEmpty.type] = request.withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )

      val result: Future[Result] = controller.whatIsYourNino(completionUrl)(req)

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.select("h1.govuk-label-wrapper").text() shouldBe messages("personal-details.faded-heading") + " " + messages("what-is-your-national-insurance-number.nino.label")
      document.select("form[method=POST]").attr("action") shouldBe routes.PersonalDetailsCollectionController.submitYourNino(completionUrl).url
      document.select("#error-summary-display .js-error-summary-messages").isEmpty shouldBe true

      val fieldsets: Elements = document.select("form")

      val ninoFieldset: Element = fieldsets.first()
      ninoFieldset.select("label[for=nino]").text() shouldBe "Check your identity What is your National Insurance number?"

      document.select("button[type=submit]").text() shouldBe messages("continue.button.text")
    }
  }

  "showPostCodeForm" should {
    "return OK with the ability to enter the Post Code" in new Setup {

      val req: FakeRequest[AnyContentAsEmpty.type] = request.withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )
      val result: Future[Result] = controller.whatIsYourPostCode(completionUrl)(req)

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.select("h1.govuk-label-wrapper").text() shouldBe messages("personal-details.faded-heading") + " " + messages("what-is-your-postcode.postcode.label")
      document.select("form[method=POST]").attr("action") shouldBe routes.PersonalDetailsCollectionController.submitYourPostCode(completionUrl).url
      document.select("#error-summary-display .js-error-summary-messages").isEmpty shouldBe true
      document.select("button[type=submit]").text() shouldBe messages("continue.button.text")
    }
  }

  "submitNino" should {
    "succeed when given a valid Nino" in new Setup {
      val validationId: ValidationId = ValidationId(UUID.randomUUID().toString)
      val expectedRedirect: Result = Redirect(completionUrl.value, Map("validationId" -> Seq(validationId.value)))

      val pdv : EitherT[Future, Result, PersonalDetailsValidation] = EitherT.rightT[Future, Result](SuccessfulPersonalDetailsValidation(validationId))

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

      (personalDetailsSubmitterMock.submitPersonalDetails(_ : PersonalDetails, _ : CompletionUrl)(_: Request[_], _: HeaderCarrier, _: ExecutionContext))
        .expects(expectedPersonalDetails, completionUrl, *, *, *)
        .returns(pdv)

      (personalDetailsSubmitterMock.successResult(_ : CompletionUrl, _ : PersonalDetailsValidation)(_: Request[_]))
        .expects(*, *, *)
        .returns(expectedRedirect)

      val result: Future[Result] = controller.submitYourNino(completionUrl)(req)

      await(result) shouldBe expectedRedirect
    }

    "redirect to showPage if no details found" in new Setup {
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

      (personalDetailsSubmitterMock.submitPersonalDetails(_ : PersonalDetails, _ : CompletionUrl)(_: Request[_], _: HeaderCarrier, _: ExecutionContext))
        .expects(expectedPersonalDetails, completionUrl, *, *, *)
        .returns(pdv)

      val result: Future[Result] = controller.submitYourNino(completionUrl)(req)

      status(result) shouldBe SEE_OTHER
      redirectLocation(await(result)).get.contains("/personal-details-validation/enter-your-details?completionUrl=") shouldBe true
    }

    "Bad Request if the initial details are not present in the request" in new Setup {

      val result: Future[Result] = controller.submitYourNino(completionUrl)(request.withFormUrlEncodedBody("nino" -> "AA000001A"))

      status(result) shouldBe BAD_REQUEST
    }
  }

  "submitPostcode" should {
    "succeed when given a valid Postcode" in new Setup {

      val validationId: ValidationId = ValidationId(UUID.randomUUID().toString)
      val expectedRedirect: Result = Redirect(completionUrl.value, Map("validationId" -> Seq(validationId.value)))

      val pdv : EitherT[Future, Result, PersonalDetailsValidation] = EitherT.rightT[Future, Result](SuccessfulPersonalDetailsValidation(validationId))

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

      (personalDetailsSubmitterMock.submitPersonalDetails(_ : PersonalDetails, _ : CompletionUrl)(_: Request[_], _: HeaderCarrier, _: ExecutionContext))
        .expects(expectedPersonalDetails, completionUrl, *, *, *)
        .returns(pdv)

      (personalDetailsSubmitterMock.successResult(_ : CompletionUrl, _ : PersonalDetailsValidation)(_: Request[_]))
        .expects(*, *, *)
        .returns(expectedRedirect)

      val result: Future[Result] = controller.submitYourPostCode(completionUrl)(req)

      await(result) shouldBe expectedRedirect
    }

    "display error field validation error nino missing" in new Setup with BindFromRequestTooling {

      val req: FakeRequest[AnyContentAsEmpty.type] = request.withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )

      val result: Future[Result] = controller.submitYourPostCode(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("validation.error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.postcode.invalid")
    }

    "display error field validation error nino invalid" in new Setup with BindFromRequestTooling {

      val req: FakeRequest[AnyContentAsFormUrlEncoded] = request.withFormUrlEncodedBody("postcode" -> "INVALID") .withSession(
        "firstName" -> "Jim",
        "lastName" -> "Ferguson",
        "dob" -> "1939-09-01"
      )

      val result: Future[Result] = controller.submitYourPostCode(completionUrl)(req)

      status(result) shouldBe OK

      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")

      val document: Document = Jsoup.parse(contentAsString(result))

      document.errorsSummary.heading shouldBe messages("validation.error-summary.heading")
      document.errorsSummary.content shouldBe messages("personal-details.postcode.invalid")
    }

    "Bad Request if the initial details are not present in the request" in new Setup {
      val result: Future[Result] = controller.submitYourPostCode(completionUrl)(request.withFormUrlEncodedBody("postcode" -> "BN11 1NN"))
      status(result) shouldBe BAD_REQUEST
    }

    "redirect to showPage if no details found" in new Setup {
      val validationId: ValidationId = ValidationId(UUID.randomUUID().toString)

      val pdv : EitherT[Future, Result, PersonalDetailsValidation] = EitherT.rightT[Future, Result](FailedPersonalDetailsValidation(validationId))

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

      (personalDetailsSubmitterMock.submitPersonalDetails(_ : PersonalDetails, _ : CompletionUrl)(_: Request[_], _: HeaderCarrier, _: ExecutionContext))
        .expects(expectedPersonalDetails, completionUrl, *, *, *)
        .returns(pdv)

      val result: Future[Result] = controller.submitYourPostCode(completionUrl)(req)

      status(result) shouldBe SEE_OTHER
      redirectLocation(await(result)).get.contains("/personal-details-validation/enter-your-details?completionUrl=") shouldBe true
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

      private val redirectUrl = s"${completionUrl.value}&userTimeout="
      (mockEventDispatcher.dispatchEvent(_: MonitoringEvent)(_: Request[_], _: HeaderCarrier, _: ExecutionContext)).expects(TimedOut, *, *, *)
      (mockIVConnector.updateJourney(_: String)(_: HeaderCarrier, _: ExecutionContext)).expects(*, *, *)

      private val result = controller.redirectAfterTimeout(completionUrl)(request)
      status(result) shouldBe 303
      redirectLocation(Await.result(result, 5 seconds)) shouldBe Some(redirectUrl)
    }

  }

  private trait Setup {

    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

    val completionUrl: CompletionUrl = ValuesGenerators.completionUrls.generateOne

    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: Materializer = Materializer.apply(system)

    implicit val dwpMessagesApiProvider: DwpMessagesApiProvider = app.injector.instanceOf[DwpMessagesApiProvider]
    implicit val lang: Lang = Lang("en-GB")
    implicit val messages: Messages = MessagesImpl(lang, dwpMessagesApiProvider.get)

    val personalDetailsSubmitterMock: FuturedPersonalDetailsSubmission = mock[FuturedPersonalDetailsSubmission]
    val mockAppConfig: AppConfig = mock[AppConfig]
    val mockEventDispatcher: EventDispatcher = mock[EventDispatcher]
    val mockIVConnector: IdentityVerificationConnector = mock[IdentityVerificationConnector]
    implicit val mockViewConfig: ViewConfig = app.injector.instanceOf[ViewConfig]

    val stubMessagesControllerComponents: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

    val enter_your_details: enter_your_details = app.injector.instanceOf[enter_your_details]
    val what_is_your_postcode: what_is_your_postcode = app.injector.instanceOf[what_is_your_postcode]
    val what_is_your_nino: what_is_your_nino = app.injector.instanceOf[what_is_your_nino]

    val we_cannot_check_your_identity: we_cannot_check_your_identity = app.injector.instanceOf[we_cannot_check_your_identity]

    implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

    implicit val authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]

    implicit val ec: ExecutionContext = ExecutionContext.global

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val controller = new PersonalDetailsCollectionController(
      personalDetailsSubmitterMock,
      mockAppConfig,
      mockEventDispatcher,
      stubMessagesControllerComponents,
      what_is_your_postcode,
      what_is_your_nino,
      enter_your_details,
      we_cannot_check_your_identity,
      mockIVConnector)
  }

  private trait BindFromRequestTooling {

    self: Setup =>

    implicit class PageOps(page: Document) {

      lazy val errorsSummary: Object {
        val heading: String
        val content: String
      } = new {
        lazy val errorsSummaryDiv: Elements = page.select("div[class=govuk-error-summary]")
        lazy val heading: String = errorsSummaryDiv.select("h2").text()
        lazy val content: String = errorsSummaryDiv.select("ul").text()
      }

    }
  }
}
