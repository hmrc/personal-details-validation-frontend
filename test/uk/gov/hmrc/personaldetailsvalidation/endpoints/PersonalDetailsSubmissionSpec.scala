/*
 * Copyright 2019 HM Revenue & Customs
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

import akka.stream.Materializer
import cats.Id
import cats.data.EitherT
import com.kenshoo.play.metrics.Metrics
import generators.Generators.Implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.HeaderNames
import play.api.mvc.Results._
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.errorhandling.ProcessingError
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.logging.Logger
import uk.gov.hmrc.personaldetailsvalidation.connectors.PersonalDetailsSender
import uk.gov.hmrc.personaldetailsvalidation.generators.ObjectGenerators.{personalDetailsObjects, successfulPersonalDetailsValidationObjects, _}
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators.completionUrls
import uk.gov.hmrc.personaldetailsvalidation.model._
import uk.gov.hmrc.personaldetailsvalidation.monitoring.PdvMetrics
import uk.gov.hmrc.personaldetailsvalidation.views.pages.PersonalDetailsPage
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.personaldetailsvalidation.model.QueryParamConverter._

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.{global => executionContext}

class PersonalDetailsSubmissionSpec
  extends UnitSpec
    with MockFactory
    with OneAppPerSuite
    with GeneratorDrivenPropertyChecks {

  import PersonalDetailsSubmission._

  "bindValidateAndRedirect" should {

    "return BAD_REQUEST when form binds with errors" in new Setup {

      val completionUrl = completionUrls.generateOne

      (page.bindFromRequest(_: Boolean)(_: Request[_], _: CompletionUrl))
        .expects(false, request, completionUrl)
        .returning(Left(Html("page with errors")))

      val result = submitter.submit(completionUrl)

      status(result) shouldBe BAD_REQUEST
      contentAsString(result) shouldBe "page with errors"
      result.session.get(validationIdSessionKey) shouldBe None
    }

    "bind the request to PersonalDetailsWithNino, " +
      "post it to the validation service, " +
      "return redirect to completionUrl with appended validationId query parameter " +
      "if post to the validation service returned successful personal details validation " +
      "and we update the GA counter for Ninos" in new Setup {

      val completionUrl = completionUrls.generateOne
      val personalDetails = personalDetailsObjects.generateOne
      val personalDetailsValidation = successfulPersonalDetailsValidationObjects.generateOne

      val ninoCounterBefore = pdvMetrics.ninoCounter
      val postCodeCounterBefore = pdvMetrics.postCodeCounter

      (page.bindFromRequest(_: Boolean)(_: Request[_], _: CompletionUrl))
        .expects(false, request, completionUrl)
        .returning(Right(personalDetails))

      (personalDetailsValidationConnector.submitValidationRequest(_: PersonalDetails)(_: HeaderCarrier, _: ExecutionContext))
        .expects(personalDetails, headerCarrier, executionContext)
        .returning(EitherT.rightT[Id, ProcessingError](personalDetailsValidation))

      val result = submitter.submit(completionUrl)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(s"${completionUrl.value}&validationId=${personalDetailsValidation.validationId}")
      result.session.get(validationIdSessionKey) shouldBe Some(personalDetailsValidation.validationId.value)

      pdvMetrics.ninoCounter shouldBe (ninoCounterBefore + 1)
      postCodeCounterBefore shouldBe pdvMetrics.postCodeCounter
    }

    "bind the request to PersonalDetailsWithPostcode, " +
      "post it to the validation service, " +
      "return redirect to completionUrl with appended validationId query parameter " +
      "if post to the validation service returned successful personal details validation " +
      "and we update the GA counter for PostCode" in new Setup {

      val completionUrl = completionUrls.generateOne
      val personalDetails = personalDetailsObjectsWithPostcode.generateOne
      val personalDetailsValidation = successfulPersonalDetailsValidationObjects.generateOne

      val ninoCounterBefore = pdvMetrics.ninoCounter
      val postCodeCounterBefore = pdvMetrics.postCodeCounter

      (page.bindFromRequest(_: Boolean)(_: Request[_], _: CompletionUrl))
        .expects(true, request, completionUrl)
        .returning(Right(personalDetails))

      (personalDetailsValidationConnector.submitValidationRequest(_: PersonalDetails)(_: HeaderCarrier, _: ExecutionContext))
        .expects(personalDetails, headerCarrier, executionContext)
        .returning(EitherT.rightT[Id, ProcessingError](personalDetailsValidation))

      val result = submitter.submit(completionUrl, true)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(s"${completionUrl.value}&validationId=${personalDetailsValidation.validationId}")
      result.session.get(validationIdSessionKey) shouldBe Some(personalDetailsValidation.validationId.value)

      ninoCounterBefore shouldBe pdvMetrics.ninoCounter
      (postCodeCounterBefore + 1) shouldBe pdvMetrics.postCodeCounter
    }

    "bind the request to PersonalDetailsWithNino, " +
      "post it to the validation service, " +
      "the service returns an error of some kind" +
      "and we do not update the GA counter for Nino" in new Setup {

      val completionUrl = completionUrls.generateOne
      val personalDetails = personalDetailsObjects.generateOne
      val usePostCode = false

      val ninoCounterBefore = pdvMetrics.ninoCounter
      val postCodeCounterBefore = pdvMetrics.postCodeCounter

      (page.bindFromRequest(_: Boolean)(_: Request[_], _: CompletionUrl))
        .expects(usePostCode, request, completionUrl)
        .returning(Right(personalDetails))

      val error = ProcessingError("some message")
      (personalDetailsValidationConnector.submitValidationRequest(_: PersonalDetails)(_: HeaderCarrier, _: ExecutionContext))
        .expects(personalDetails, headerCarrier, executionContext)
        .returning(EitherT.leftT[Id, PersonalDetailsValidation](error))

      (logger.error(_: ProcessingError)).expects(error)

      val result = submitter.submit(completionUrl, usePostCode)

      ninoCounterBefore shouldBe pdvMetrics.ninoCounter
      postCodeCounterBefore shouldBe pdvMetrics.postCodeCounter
    }

    "bind the request to PersonalDetailsWithPostcode, " +
      "post it to the validation service, " +
      "the service returns an error of some kind" +
      "and we do not update the GA counter for PostCode" in new Setup {

      val completionUrl = completionUrls.generateOne
      val personalDetails = personalDetailsObjects.generateOne
      val usePostCode = true

      val ninoCounterBefore = pdvMetrics.ninoCounter
      val postCodeCounterBefore = pdvMetrics.postCodeCounter

      (page.bindFromRequest(_: Boolean)(_: Request[_], _: CompletionUrl))
        .expects(usePostCode, request, completionUrl)
        .returning(Right(personalDetails))

      val error = ProcessingError("some message")
      (personalDetailsValidationConnector.submitValidationRequest(_: PersonalDetails)(_: HeaderCarrier, _: ExecutionContext))
        .expects(personalDetails, headerCarrier, executionContext)
        .returning(EitherT.leftT[Id, PersonalDetailsValidation](error))

      (logger.error(_: ProcessingError)).expects(error)

      val result = submitter.submit(completionUrl, usePostCode)

      ninoCounterBefore shouldBe pdvMetrics.ninoCounter
      postCodeCounterBefore shouldBe pdvMetrics.postCodeCounter
    }

    val usePostcodeFormOptions = List(true, false)

    usePostcodeFormOptions.foreach { usePostcodeForm =>

      "bind the request to PersonalDetails, " +
        "post it to the validation service, " +
        "render the form with error" +
        s"if post to the validation service returned failed personal details validation and usePostcodeForm=$usePostcodeForm" in new Setup {

        val completionUrl = completionUrls.generateOne
        val personalDetails = personalDetailsObjects.generateOne
        val failedPersonalDetailsValidation = failedPersonalDetailsValidationObjects.generateOne
        val completionUrlWithValidationId = CompletionUrl(Redirect(completionUrl.value, failedPersonalDetailsValidation.validationId.toQueryParam).header.headers.getOrElse(HeaderNames.LOCATION, completionUrl.value))

        (page.bindFromRequest(_: Boolean)(_: Request[_], _: CompletionUrl))
          .expects(usePostcodeForm, request, completionUrl)
          .returning(Right(personalDetails))

        (personalDetailsValidationConnector.submitValidationRequest(_: PersonalDetails)(_: HeaderCarrier, _: ExecutionContext))
          .expects(personalDetails, headerCarrier, executionContext)
          .returning(EitherT.rightT[Id, ProcessingError](failedPersonalDetailsValidation))

        val html = Html("OK")

        (page.renderValidationFailure(_: Boolean)(_: CompletionUrl, _: Request[_]))
          .expects(usePostcodeForm, completionUrlWithValidationId, request)
          .returning(html)

        val result = submitter.submit(completionUrl, usePostcodeForm)

        status(result) shouldBe OK
        bodyOf(result) shouldBe html.toString()

        result.session.get(validationIdSessionKey) shouldBe Some(failedPersonalDetailsValidation.validationId.value)
      }
    }

    usePostcodeFormOptions.foreach { usePostcodeForm =>

      "bind the request to PersonalDetails, " +
        "post it to the validation service and " +
        "return redirect to completionUrl with appended 'technicalError' query parameter " +
        s"if post to the validation service fails and usePostcodeForm=$usePostcodeForm" in new Setup {

        val completionUrl = completionUrls.generateOne
        val personalDetails = personalDetailsObjects.generateOne
        (page.bindFromRequest(_: Boolean)(_: Request[_], _: CompletionUrl))
          .expects(usePostcodeForm, request, completionUrl)
          .returning(Right(personalDetails))

        val error = ProcessingError("some message")
        (personalDetailsValidationConnector.submitValidationRequest(_: PersonalDetails)(_: HeaderCarrier, _: ExecutionContext))
          .expects(personalDetails, headerCarrier, executionContext)
          .returning(EitherT.leftT[Id, PersonalDetailsValidation](error))

        (logger.error(_: ProcessingError)).expects(error)

        val result = submitter.submit(completionUrl, usePostcodeForm)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"${completionUrl.value}&technicalError=")

        result.session.get(validationIdSessionKey) shouldBe None
      }
    }
  }

  private trait Setup {
    implicit val request: Request[AnyContentAsEmpty.type] = FakeRequest()
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
    implicit val materializer: Materializer = mock[Materializer]

    val page = mock[PersonalDetailsPage]
    val personalDetailsValidationConnector = mock[PersonalDetailsSender[Id]]
    val logger = mock[Logger]
    val metrics = mock[Metrics]
    val pdvMetrics = new MockPdvMetrics

    class MockPdvMetrics extends PdvMetrics(metrics) {
      var ninoCounter = 0
      var postCodeCounter = 0
      var errorCounter = 0
      override def matchPersonalDetails(details: PersonalDetails): Boolean = {
        details match {
          case _ : PersonalDetailsWithNino =>
            ninoCounter += 1
            true
          case _ : PersonalDetailsWithPostcode =>
            postCodeCounter += 1
            true
          case _ =>
            errorCounter += 1
            false
        }
      }
    }

    val submitter = new PersonalDetailsSubmission[Id](page, personalDetailsValidationConnector, pdvMetrics, logger)
  }
}
