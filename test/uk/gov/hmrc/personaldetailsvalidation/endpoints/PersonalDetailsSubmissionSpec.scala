/*
 * Copyright 2018 HM Revenue & Customs
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

import cats.Id
import cats.data.EitherT
import generators.Generators.Implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.OneAppPerSuite
import play.api.mvc.Results._
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.errorhandling.ProcessingError
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.logging.Logger
import uk.gov.hmrc.personaldetailsvalidation.connectors.PersonalDetailsSender
import uk.gov.hmrc.personaldetailsvalidation.generators.ObjectGenerators.{personalDetailsObjects, successfulPersonalDetailsValidationObjects}
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators.completionUrls
import uk.gov.hmrc.personaldetailsvalidation.model.{CompletionUrl, FailedPersonalDetailsValidation, PersonalDetails, PersonalDetailsValidation}
import uk.gov.hmrc.personaldetailsvalidation.views.pages.PersonalDetailsPage
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.{global => executionContext}

class PersonalDetailsSubmissionSpec
  extends UnitSpec
    with MockFactory
    with OneAppPerSuite {

  import PersonalDetailsSubmission._

  "bindValidateAndRedirect" should {

    "return BAD_REQUEST when form binds with errors" in new Setup {

      val completionUrl = completionUrls.generateOne

      (page.bindFromRequest(_: Request[_], _: CompletionUrl))
        .expects(request, completionUrl)
        .returning(Left(Html("page with errors")))

      val result = submitter.submit(completionUrl)

      status(result) shouldBe BAD_REQUEST
      contentAsString(result) shouldBe "page with errors"
      result.session.get(validationIdSessionKey) shouldBe None
    }

    "bind the request to PersonalDetails, " +
      "post it to the validation service, " +
      "return redirect to completionUrl with appended validationId query parameter" +
      "if post to the validation service returned successful personal details validation" in new Setup {

      val completionUrl = completionUrls.generateOne
      val personalDetails = personalDetailsObjects.generateOne
      val personalDetailsValidation = successfulPersonalDetailsValidationObjects.generateOne
      (page.bindFromRequest(_: Request[_], _: CompletionUrl))
        .expects(request, completionUrl)
        .returning(Right(personalDetails))

      (personalDetailsValidationConnector.submitValidationRequest(_: PersonalDetails)(_: HeaderCarrier, _: ExecutionContext))
        .expects(personalDetails, headerCarrier, executionContext)
        .returning(EitherT.rightT[Id, ProcessingError](personalDetailsValidation))

      val result = submitter.submit(completionUrl)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(s"${completionUrl.value}&validationId=${personalDetailsValidation.validationId}")
      result.session.get(validationIdSessionKey) shouldBe Some(personalDetailsValidation.validationId.value)
    }

    "bind the request to PersonalDetails, " +
      "post it to the validation service, " +
      "render the form with error" +
      "if post to the validation service returned failed personal details validation" in new Setup {

      val completionUrl = completionUrls.generateOne
      val personalDetails = personalDetailsObjects.generateOne

      (page.bindFromRequest(_: Request[_], _: CompletionUrl))
        .expects(request, completionUrl)
        .returning(Right(personalDetails))

      (personalDetailsValidationConnector.submitValidationRequest(_: PersonalDetails)(_: HeaderCarrier, _: ExecutionContext))
        .expects(personalDetails, headerCarrier, executionContext)
        .returning(EitherT.rightT[Id, ProcessingError](FailedPersonalDetailsValidation))

      val html = Html("OK")

      (page.renderValidationFailure(_: CompletionUrl, _: Request[_]))
        .expects(completionUrl, request)
        .returning(html)

      val result = submitter.submit(completionUrl)
      result shouldBe Ok(html)
      result.session.get(validationIdSessionKey) shouldBe None
    }

    "bind the request to PersonalDetails, " +
      "post it to the validation service and " +
      "return redirect to completionUrl with appended 'technicalError' query parameter " +
      "if post to the validation service fails" in new Setup {

      val completionUrl = completionUrls.generateOne
      val personalDetails = personalDetailsObjects.generateOne
      (page.bindFromRequest(_: Request[_], _: CompletionUrl))
        .expects(request, completionUrl)
        .returning(Right(personalDetails))

      val error = ProcessingError("some message")
      (personalDetailsValidationConnector.submitValidationRequest(_: PersonalDetails)(_: HeaderCarrier, _: ExecutionContext))
        .expects(personalDetails, headerCarrier, executionContext)
        .returning(EitherT.leftT[Id, PersonalDetailsValidation](error))

      (logger.error(_: ProcessingError)).expects(error)

      val result = submitter.submit(completionUrl)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(s"${completionUrl.value}&technicalError=")

      result.session.get(validationIdSessionKey) shouldBe None
    }
  }

  private trait Setup {
    implicit val request: Request[AnyContentAsEmpty.type] = FakeRequest()
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

    val page = mock[PersonalDetailsPage]
    val personalDetailsValidationConnector = mock[PersonalDetailsSender[Id]]
    val logger = mock[Logger]

    val submitter = new PersonalDetailsSubmission[Id](page, personalDetailsValidationConnector, logger)
  }
}
