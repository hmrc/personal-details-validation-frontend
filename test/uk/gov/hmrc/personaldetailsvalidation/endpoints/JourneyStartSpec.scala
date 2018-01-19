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
import generators.Generators.nonEmptyStrings
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.OneAppPerSuite
import play.api.LoggerLike
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import uk.gov.hmrc.errorhandling.ProcessingError
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.logging.Logger
import uk.gov.hmrc.personaldetailsvalidation.connectors.ValidationIdValidator
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators
import uk.gov.hmrc.personaldetailsvalidation.model.CompletionUrl
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.{global => executionContext}

class JourneyStartSpec
  extends UnitSpec
    with OneAppPerSuite
    with MockFactory {

  import PersonalDetailsSubmission._

  "findRedirect" should {

    "return redirect to the GET /personal-details if there's no 'validationId' in the session" in new Setup {
      journeyStart.findRedirect(completionUrl) shouldBe Redirect(routes.PersonalDetailsCollectionController.showPage(completionUrl))
    }

    "return redirect to the given completionUrl with 'validationId' appended as a query parameter " +
      "if there's a 'validationId' in the session " +
      "and it's valid" in new Setup {
      implicit val requestWithValidationId = request.withSession(validationIdSessionKey -> validationId)

      (validationIdValidator.verify(_: String)(_: HeaderCarrier, _: ExecutionContext))
        .expects(validationId, headerCarrier, executionContext)
        .returning(EitherT.rightT[Id, ProcessingError](true))

      val redirect = Redirect("redirect-url")
      (redirectComposer.redirect(_: CompletionUrl, _: String))
        .expects(completionUrl, validationId)
        .returning(redirect)

      journeyStart.findRedirect(completionUrl) shouldBe redirect
    }

    "return redirect to the GET /personal-details " +
      "if there's 'validationId' in the session " +
      "but it's not valid" in new Setup {
      implicit val requestWithValidationId = request.withSession(validationIdSessionKey -> validationId)

      (validationIdValidator.verify(_: String)(_: HeaderCarrier, _: ExecutionContext))
        .expects(validationId, headerCarrier, executionContext)
        .returning(EitherT.rightT[Id, ProcessingError](false))

      journeyStart.findRedirect(completionUrl) shouldBe Redirect(routes.PersonalDetailsCollectionController.showPage(completionUrl))
    }

    "log an validation error and return redirect to the given completionUrl with 'technicalError' " +
      "if there's 'validationId' in the session " +
      "but 'validationId' validation returns an error" in new Setup {
      implicit val requestWithValidationId = request.withSession(validationIdSessionKey -> validationId)

      val validationError = ProcessingError("some message")
      (validationIdValidator.verify(_: String)(_: HeaderCarrier, _: ExecutionContext))
        .expects(validationId, headerCarrier, executionContext)
        .returning(EitherT.leftT[Id, Boolean](validationError))

      (logger.error(_: ProcessingError))
        .expects(validationError)

      val redirect = Redirect("redirect-url")
      (redirectComposer.redirectWithTechnicalErrorParameter(_: CompletionUrl))
        .expects(completionUrl)
        .returning(redirect)

      journeyStart.findRedirect(completionUrl) shouldBe redirect
    }
  }

  private trait Setup {
    implicit val request = FakeRequest()
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

    val completionUrl = ValuesGenerators.completionUrls.generateOne
    val validationId = nonEmptyStrings.generateOne

    val validationIdValidator = mock[ValidationIdValidator[Id]]
    val redirectComposer = mock[RedirectComposer]
    val logger = mock[Logger]

    val journeyStart = new JourneyStart[Id](validationIdValidator, redirectComposer, logger)
  }
}
