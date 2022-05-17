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

import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import support.Generators.Implicits._
import support.UnitSpec
import uk.gov.hmrc.errorhandling.ProcessingError
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.logging.Logger
import uk.gov.hmrc.personaldetailsvalidation.connectors.ValidationIdValidator
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators.{completionUrls, validationIds}
import uk.gov.hmrc.personaldetailsvalidation.model.{CompletionUrl, ValidationId}

import scala.concurrent.ExecutionContext.Implicits.{global => executionContext}
import scala.concurrent.{ExecutionContext, Future}

class JourneyStartSpec extends UnitSpec with GuiceOneAppPerSuite with MockFactory {

  val validationIdSessionKey = "ValidationId"

  "findRedirect" should {

    "return redirect to the GET /personal-details if there's no 'validationId' in the session" in new Setup {
      await(journeyStart.findRedirect(completionUrl, origin, failureUrl)) shouldBe
        Redirect(routes.PersonalDetailsCollectionController.showPage(completionUrl, origin))
    }

    "return redirect to the given completionUrl with 'validationId' appended as a query parameter " +
      "if there's a 'validationId' in the session " +
      "and it's valid" in new Setup {
      implicit val requestWithValidationId: FakeRequest[AnyContentAsEmpty.type] = request.withSession(validationIdSessionKey -> validationId.value)

      (validationIdValidator.checkExists(_: ValidationId)(_: HeaderCarrier, _: ExecutionContext))
        .expects(validationId, headerCarrier, executionContext)
        .returning(Future.successful(true))

      await(journeyStart.findRedirect(completionUrl, origin, failureUrl)) shouldBe Redirect(completionUrl.value, Map("validationId" -> Seq(validationId.value)))
    }

    "return redirect to the GET /personal-details " +
      "if there's 'validationId' in the session " +
      "but it's not valid" in new Setup {
      implicit val requestWithValidationId: FakeRequest[AnyContentAsEmpty.type] = request.withSession(validationIdSessionKey -> validationId.value)

      (validationIdValidator.checkExists(_: ValidationId)(_: HeaderCarrier, _: ExecutionContext))
        .expects(validationId, headerCarrier, executionContext)
        .returning(Future.successful(false))

      await(journeyStart.findRedirect(completionUrl, origin, failureUrl)) shouldBe Redirect(routes.PersonalDetailsCollectionController.showPage(completionUrl, origin))
    }

    "log an validation error and return redirect to the given completionUrl with 'technicalError' " +
      "if there's 'validationId' in the session " +
      "but 'validationId' validation returns an error" in new Setup {
      implicit val requestWithValidationId: FakeRequest[AnyContentAsEmpty.type] = request.withSession(validationIdSessionKey -> validationId.value)

      val validationError: ProcessingError = ProcessingError("some message")

      (validationIdValidator.checkExists(_: ValidationId)(_: HeaderCarrier, _: ExecutionContext))
        .expects(validationId, headerCarrier, executionContext)
        .returning(Future.failed(new RuntimeException(validationError.message)))

      (logger.error(_: ProcessingError)).expects(ProcessingError("Unable to start this journey: some message"))

      val result: Future[Result] = journeyStart.findRedirect(completionUrl, origin, failureUrl)
      status(result) shouldBe SEE_OTHER
      redirectLocation(await(result)) shouldBe Some(s"${completionUrl.value}&technicalError=")
    }
  }

  private trait Setup {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

    val origin: Option[String] = Some("test")

    val completionUrl: CompletionUrl = completionUrls.generateOne
    val failureUrl: Option[CompletionUrl] = None
    val validationId: ValidationId = validationIds.generateOne

    val validationIdValidator: ValidationIdValidator = mock[ValidationIdValidator]
    val logger: Logger = mock[Logger]

    val journeyStart = new JourneyStart(validationIdValidator, logger)
  }
}
