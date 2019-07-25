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

import java.util.UUID

import cats.Monad
import cats.data.EitherT
import cats.implicits._
import play.api.http.HeaderNames
import play.api.mvc.Results._
import play.api.mvc.{Request, Result}
import play.twirl.api.Html
import uk.gov.hmrc.errorhandling.ProcessingError
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.logging.Logger
import uk.gov.hmrc.personaldetailsvalidation.connectors.{FuturedPersonalDetailsSender, PersonalDetailsSender}
import uk.gov.hmrc.personaldetailsvalidation.model.QueryParamConverter._
import uk.gov.hmrc.personaldetailsvalidation.model.{CompletionUrl, FailedPersonalDetailsValidation, PersonalDetailsValidation, SuccessfulPersonalDetailsValidation, ValidationId}
import uk.gov.hmrc.personaldetailsvalidation.monitoring.PdvMetrics
import uk.gov.hmrc.personaldetailsvalidation.views.pages.PersonalDetailsPage
import javax.inject.{Inject, Singleton}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}

@Singleton
private class FuturedPersonalDetailsSubmission @Inject()(personalDetailsPage: PersonalDetailsPage,
                                                         personalDetailsValidationConnector: FuturedPersonalDetailsSender,
                                                         pdvMetrics: PdvMetrics,
                                                         logger: Logger)
  extends PersonalDetailsSubmission[Future](personalDetailsPage, personalDetailsValidationConnector, pdvMetrics, logger)

private class PersonalDetailsSubmission[Interpretation[_] : Monad](personalDetailsPage: PersonalDetailsPage,
                                                                   personalDetailsValidationConnector: PersonalDetailsSender[Interpretation],
                                                                   pdvMetrics: PdvMetrics,
                                                                   logger: Logger) {

  import PersonalDetailsSubmission._
  import personalDetailsValidationConnector._

  def submit(completionUrl: CompletionUrl, usePostcodeForm: Boolean = false)
            (implicit request: Request[_],
                              headerCarrier: HeaderCarrier,
                              executionContext: ExecutionContext): Interpretation[Result] = {
    for {
      personalDetails <- pure(personalDetailsPage.bindFromRequest(usePostcodeForm)(request, completionUrl)) leftMap pageWithErrorToBadRequest
      personalDetailsValidation <- submitValidationRequest(personalDetails) leftMap errorToRedirect(to = completionUrl)
      counterUpdated = pdvMetrics.matchPersonalDetails(personalDetails)
    } yield result(completionUrl, personalDetailsValidation, usePostcodeForm)
  }.merge

  private val pageWithErrorToBadRequest: Html => Result = BadRequest(_)

  private def errorToRedirect(to: CompletionUrl): ProcessingError => Result = {
    error =>
      logger.error(error)
      Redirect(to.value, error.toQueryParam)
  }

  private val UUIDRegex = """[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89aAbB][a-f0-9]{3}-[a-f0-9]{12}"""

  private def stripValidationId(redirectUrl: String): String =
    redirectUrl.replaceAll(s"""[?&]validationId=$UUIDRegex""", "")

  private def result(completionUrl: CompletionUrl, personalDetailsValidation: PersonalDetailsValidation, usePostcodeForm: Boolean = false)
                    (implicit request: Request[_]): Result = {
    val strippedCompletionUrl = stripValidationId(completionUrl.value)
    personalDetailsValidation match {
      case SuccessfulPersonalDetailsValidation(validationId) =>
        Redirect(strippedCompletionUrl, validationId.toQueryParam).addingToSession(validationIdSessionKey -> validationId.value)
      case FailedPersonalDetailsValidation(validationId) =>
        val redirectUrl = Redirect(strippedCompletionUrl,validationId.toQueryParam).header.headers.getOrElse(HeaderNames.LOCATION, strippedCompletionUrl)
        Ok(personalDetailsPage.renderValidationFailure(usePostcodeForm)(CompletionUrl(redirectUrl), request)).addingToSession(validationIdSessionKey -> validationId.value)
    }
  }
  private def pure[L, R](maybeValue: Either[L, R]): EitherT[Interpretation, L, R] =
    EitherT(implicitly[Monad[Interpretation]].pure(maybeValue))
}

private object PersonalDetailsSubmission {
  val validationIdSessionKey = "ValidationId"
}
