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

import play.api.mvc.Results._
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.personaldetailsvalidation.connectors.PersonalDetailsSender
import uk.gov.hmrc.personaldetailsvalidation.model.QueryParamConverter._
import uk.gov.hmrc.personaldetailsvalidation.model._
import uk.gov.hmrc.personaldetailsvalidation.monitoring.PdvMetrics

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PersonalDetailsSubmission @Inject()(personalDetailsValidationConnector: PersonalDetailsSender,
                                          pdvMetrics: PdvMetrics)(implicit ec: ExecutionContext){

  val validationIdSessionKey = "ValidationId"

  def submitPersonalDetails(personalDetails: PersonalDetails)
                           (implicit request: Request[_],
                            headerCarrier: HeaderCarrier): Future[PersonalDetailsValidation] = {
    val origin = request.session.get("origin").getOrElse("Unknown-Origin")
    for {
      personalDetailsValidation <- personalDetailsValidationConnector.submitValidationRequest(personalDetails, origin)
      _ = pdvMetrics.matchPersonalDetails(personalDetails)
    } yield personalDetailsValidation
  }

  private val UUIDRegex = """[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89aAbB][a-f0-9]{3}-[a-f0-9]{12}"""

  private def stripValidationId(redirectUrl: String): String =
    redirectUrl.replaceAll(s"""[?&]validationId=$UUIDRegex""", "")

  def successResult(completionUrl: CompletionUrl, personalDetailsValidation: PersonalDetailsValidation)
                   (implicit request: Request[_]): Result = {
    val strippedCompletionUrl = stripValidationId(completionUrl.value)
    personalDetailsValidation match {
      case SuccessfulPersonalDetailsValidation(validationId, _) =>
        Redirect(strippedCompletionUrl, validationId.toQueryParam).addingToSession(validationIdSessionKey -> validationId.value)
      case _ => throw new scala.RuntimeException("Unable to redirect success validation")
    }
  }

  def getUserAttempts()(implicit headerCarrier: HeaderCarrier): Future[UserAttemptsDetails] = {
    personalDetailsValidationConnector.getUserAttempts()
  }

}
