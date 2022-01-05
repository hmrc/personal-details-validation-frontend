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

import cats.Monad
import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.logging.Logger
import uk.gov.hmrc.personaldetailsvalidation.connectors.{FuturedValidationIdValidator, ValidationIdValidator}
import uk.gov.hmrc.personaldetailsvalidation.model.QueryParamConverter._
import uk.gov.hmrc.personaldetailsvalidation.model.{CompletionUrl, ValidationId}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}

@Singleton
private class FuturedJourneyStart @Inject()(validationIdValidator: FuturedValidationIdValidator,
                                            logger: Logger)
                                           (implicit ec: ExecutionContext)
  extends JourneyStart[Future](validationIdValidator, logger)

private class JourneyStart[Interpretation[_] : Monad](validationIdValidator: ValidationIdValidator[Interpretation],
                                                      logger: Logger)
                                                     (implicit ec: ExecutionContext) {

  import validationIdValidator._

  val validationIdSessionKey = "ValidationId"

  def findRedirect(completionUrl: CompletionUrl, origin: Option[String])
                  (implicit request: Request[_], headerCarrier: HeaderCarrier): Interpretation[Result] =
    findValidationIdInSession match {
      case None =>
        Redirect(routes.PersonalDetailsCollectionController.showPage(completionUrl, origin))
      case Some(sessionValidationId) =>
        verify(sessionValidationId)
          .map(findRedirectUsing(_, sessionValidationId, completionUrl, origin))
          .valueOr { error =>
            logger.error(error)
            Redirect(completionUrl.value, error.toQueryParam)
          }
    }

  private def findValidationIdInSession(implicit request: Request[_]): Option[ValidationId] =
    request.session.get(validationIdSessionKey).map(ValidationId(_))

  private def findRedirectUsing(validationResult: Boolean, validationId: ValidationId,
                                completionUrl: CompletionUrl, origin: Option[String]): Result =
    validationResult match {
      case false => Redirect(routes.PersonalDetailsCollectionController.showPage(completionUrl, origin))
      case true => Redirect(completionUrl.value, validationId.toQueryParam)
    }

  private implicit def pure[R](value: R): Interpretation[R] =
    implicitly[Monad[Interpretation]].pure(value)
}
