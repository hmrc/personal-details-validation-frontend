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

import javax.inject.{Inject, Singleton}

import cats.Monad
import cats.data.OptionT
import cats.implicits._
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.personaldetailsvalidation.connectors.{FuturedValidationIdValidator, ValidationIdValidator}
import uk.gov.hmrc.personaldetailsvalidation.model.CompletionUrl

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}

@Singleton
private class FuturedJourneyStart @Inject()(validationIdValidator: FuturedValidationIdValidator,
                                            redirectComposer: RedirectComposer)
  extends JourneyStart[Future](validationIdValidator, redirectComposer)

private class JourneyStart[Interpretation[_] : Monad](validationIdValidator: ValidationIdValidator[Interpretation],
                                                      redirectComposer: RedirectComposer) {

  import PersonalDetailsSubmission._

  def findRedirect(completionUrl: CompletionUrl)
                  (implicit request: Request[_], headerCarrier: HeaderCarrier, executionContext: ExecutionContext): Interpretation[Result] = for {
    maybeSessionValidationId <- findValidationIdInSession
    maybeValidatedValidationId <- validate(maybeSessionValidationId)
    redirect <- findRedirectUsing(maybeValidatedValidationId, completionUrl)
  } yield redirect

  private def findValidationIdInSession(implicit request: Request[_]): Interpretation[Option[String]] =
    request.session.get(validationIdSessionKey)

  private def validate(maybeValidationId: Option[String])
                      (implicit headerCarrier: HeaderCarrier, executionContext: ExecutionContext): Interpretation[Option[String]] = {
    for {
      validationId <- OptionT(pure(maybeValidationId))
      verification <- OptionT.liftF(validationIdValidator.verify(validationId))
      validatedValidationId <- (verification -> validationId).toValidatedValidationId
    } yield validatedValidationId
  }.value

  private implicit class VerificationOps(verificationValidationIdTuple: (Boolean, String)) {

    private val (verification, validationId) = verificationValidationIdTuple

    val toValidatedValidationId: OptionT[Interpretation, String] =
      if (verification) OptionT.pure(validationId)
      else OptionT.none
  }

  private def findRedirectUsing(maybeValidationId: Option[String],
                                completionUrl: CompletionUrl): Interpretation[Result] = maybeValidationId match {
    case None => Redirect(routes.PersonalDetailsCollectionController.showPage(completionUrl))
    case Some(validationId) => redirectComposer.compose(completionUrl, validationId)
  }

  private implicit def pure[R](value: R): Interpretation[R] =
    implicitly[Monad[Interpretation]].pure(value)
}
