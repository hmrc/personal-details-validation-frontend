/*
 * Copyright 2017 HM Revenue & Customs
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

import java.net.URI
import javax.inject.{Inject, Singleton}

import cats.Monad
import cats.data.EitherT
import cats.implicits._
import play.api.mvc.Results._
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.personaldetailsvalidation.connectors.{FuturedPersonalDetailsSender, FuturedValidationIdFetcher, PersonalDetailsSender, ValidationIdFetcher}
import uk.gov.hmrc.personaldetailsvalidation.model.{CompletionUrl, PersonalDetails}
import uk.gov.hmrc.personaldetailsvalidation.views.pages.PersonalDetailsPage

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}

@Singleton
private class FuturedPersonalDetailsSubmission @Inject()(personalDetailsPage: PersonalDetailsPage,
                                                         personalDetailsValidationConnector: FuturedPersonalDetailsSender,
                                                         validationIdFetcher: FuturedValidationIdFetcher)
  extends PersonalDetailsSubmission[Future](personalDetailsPage, personalDetailsValidationConnector, validationIdFetcher)

private class PersonalDetailsSubmission[Interpretation[_] : Monad](personalDetailsPage: PersonalDetailsPage,
                                                                   personalDetailsValidationConnector: PersonalDetailsSender[Interpretation],
                                                                   validationIdFetcher: ValidationIdFetcher[Interpretation]) {

  def bindValidateAndRedirect(completionUrl: CompletionUrl)
                             (implicit request: Request[_],
                              headerCarrier: HeaderCarrier,
                              executionContext: ExecutionContext): Interpretation[Result] = {
    for {
      personalDetails <- pure(personalDetailsPage.bindFromRequest(request, completionUrl))
      validationIdFetchUri <- passToValidation(personalDetails)
      validationId <- fetchValidationId(validationIdFetchUri)
    } yield validationId
  }.value.fold(identity, formRedirect(completionUrl))

  private def passToValidation(personalDetails: PersonalDetails)
                              (implicit headerCarrier: HeaderCarrier,
                               executionContext: ExecutionContext): EitherT[Interpretation, Result, URI] =
    personalDetailsValidationConnector.passToValidation(personalDetails).map(Either.right[Result, URI])

  private def fetchValidationId(validationIdFetchUri: URI)
                               (implicit headerCarrier: HeaderCarrier,
                                executionContext: ExecutionContext): EitherT[Interpretation, Result, String] =
    validationIdFetcher.fetchValidationId(validationIdFetchUri).map(Either.right[Result, String])

  private def formRedirect(completionUrl: CompletionUrl)(validationId: String): Result =
    Redirect(s"$completionUrl?validationId=$validationId")

  private def pure[L, R](maybeValue: Either[L, R]): EitherT[Interpretation, L, R] =
    EitherT(implicitly[Monad[Interpretation]].pure(maybeValue))

  private implicit def toEitherT[L, R](wrappedMaybeValue: Interpretation[Either[L, R]]): EitherT[Interpretation, L, R] =
    EitherT(wrappedMaybeValue)
}
