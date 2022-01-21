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

  def findRedirect(completionUrl: CompletionUrl, origin: Option[String])
                  (implicit request: Request[_], headerCarrier: HeaderCarrier): Future[Result] =
    findValidationIdInSession match {
      case None =>
        Future.successful(Redirect(routes.PersonalDetailsCollectionController.showPage(completionUrl, origin)))
      case Some(sessionValidationId) =>
        verify(sessionValidationId)
          .map(findRedirectUsing(_, sessionValidationId, completionUrl, origin))
          .recover {
            case error: Throwable =>
              val processingError = ProcessingError(error.getMessage)
              logger.error(processingError)
              Redirect(completionUrl.value, processingError.toQueryParam)
          }
    }

  private def findValidationIdInSession(implicit request: Request[_]): Option[ValidationId] =
    request.session.get(validationIdSessionKey).map(ValidationId(_))

  private def findRedirectUsing(validationResult: Boolean, validationId: ValidationId,
                                completionUrl: CompletionUrl, origin: Option[String]): Result =
    if (validationResult) {
      Redirect(completionUrl.value, validationId.toQueryParam)
    } else {
      Redirect(routes.PersonalDetailsCollectionController.showPage(completionUrl, origin))
    }
}
