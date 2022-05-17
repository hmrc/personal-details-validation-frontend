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

import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.errorhandling.ProcessingError
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.logging.Logger
import uk.gov.hmrc.personaldetailsvalidation.connectors.ValidationIdValidator
import uk.gov.hmrc.personaldetailsvalidation.model.QueryParamConverter._
import uk.gov.hmrc.personaldetailsvalidation.model.{CompletionUrl, ValidationId}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JourneyStart @Inject()(validationIdValidator: ValidationIdValidator,
                             logger: Logger)(implicit ec: ExecutionContext) {

  import validationIdValidator._

  val validationIdSessionKey = "ValidationId"

  /** TODO review this code - it seems to be handling a situation where IV *sometimes* calls PDV twice,
    * and handling this by storing the validationId and routing the user back to completion if subsequently called
    * we should really be fixing this in IV instead!  See VER-2281
    */

  /**
    * Redirect to /personal-details (the start of a new journey) if no validationId in session
    * Redirect to /personal-details (the start of a new journey) if validationId exists in session but DOES NOT exist in BE (expired, incorrect, etc)
    *
    * Redirect to completionUrl if this journey has ALREADY been done recently (validationId in session AND exists in BE)
    *
    * If there are any exceptions thrown, redirect to failureUrl (if defined), otherwise completionUrl with a technicalError param
    */
  def findRedirect(completionUrl: CompletionUrl, origin: Option[String], failureUrl: Option[CompletionUrl])
                  (implicit request: Request[_], headerCarrier: HeaderCarrier): Future[Result] =
    findValidationIdInSession match {
      case None =>
        Future.successful(Redirect(routes.PersonalDetailsCollectionController.showPage(completionUrl, origin, failureUrl)))
      case Some(sessionValidationId) =>
        checkExists(sessionValidationId)
          .map(findRedirectUsing(_, sessionValidationId, completionUrl, origin, failureUrl))
          .recover {
            case error: Throwable =>
              val processingError = ProcessingError("Unable to start this journey: " + error.getMessage)
              logger.error(processingError)
              val redirectUrl: String = if (failureUrl.isDefined) {failureUrl.get.value} else {completionUrl.value}
              Redirect(redirectUrl, processingError.toQueryParam)
          }
    }

  /**
    * PDV sets the validationId into the session ON SUBMIT of the PDV form.
    * This effectively CACHES the the PDV journey info (IV seems to be trying to do PDV TWICE in some flows - we should fix this!!)
    * Also: when do we *remove* the validationId from the session??  Never?
    */
  private def findValidationIdInSession(implicit request: Request[_]): Option[ValidationId] =
    request.session.get(validationIdSessionKey).map(ValidationId(_))

  private def findRedirectUsing(validationIdExists: Boolean, validationId: ValidationId,
                                completionUrl: CompletionUrl, origin: Option[String], failureUrl: Option[CompletionUrl]): Result =
    if (validationIdExists) {
      Redirect(completionUrl.value, validationId.toQueryParam) // TODO what if it was a *failure* result?
    } else {
      Redirect(routes.PersonalDetailsCollectionController.showPage(completionUrl, origin, failureUrl))
    }
}
