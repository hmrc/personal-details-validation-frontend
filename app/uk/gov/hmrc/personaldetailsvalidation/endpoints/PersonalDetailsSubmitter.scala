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

import javax.inject.{Inject, Singleton}

import cats.Monad
import cats.implicits._
import play.api.mvc.Results._
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.personaldetailsvalidation.connectors.{FuturedPersonalDetailsValidationConnector, PersonalDetailsValidationConnector}
import uk.gov.hmrc.personaldetailsvalidation.model.{CompletionUrl, PersonalDetails}
import uk.gov.hmrc.personaldetailsvalidation.views.pages.PersonalDetailsPage

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}

@Singleton
private class FuturedPersonalDetailsSubmitter @Inject()(personalDetailsPage: PersonalDetailsPage,
                                                        personalDetailsValidationConnector: FuturedPersonalDetailsValidationConnector)
  extends PersonalDetailsSubmitter[Future](personalDetailsPage, personalDetailsValidationConnector)

private class PersonalDetailsSubmitter[Interpretation[_] : Monad](personalDetailsPage: PersonalDetailsPage,
                                                                  personalDetailsValidationConnector: PersonalDetailsValidationConnector[Interpretation]) {

  def bindAndSend(completionUrl: CompletionUrl)
                 (implicit request: Request[_],
                  headerCarrier: HeaderCarrier,
                  executionContext: ExecutionContext): Interpretation[Result] = for {
    maybePersonalDetails <- pure(personalDetailsPage.bind)
    maybeValidationId <- passToValidation(maybePersonalDetails)
    maybeRedirect <- formRedirect(completionUrl, maybeValidationId)
  } yield maybeRedirect.fold(identity, identity)

  private def passToValidation(maybePersonalDetails: Either[Result, PersonalDetails])
                              (implicit headerCarrier: HeaderCarrier,
                               executionContext: ExecutionContext): Interpretation[Either[Result, String]] =
    maybePersonalDetails match {
      case Right(personalDetails) => personalDetailsValidationConnector.passToValidation(personalDetails).map(Right(_))
      case Left(badRequest) => Left(badRequest)
    }

  private def formRedirect(completionUrl: CompletionUrl, maybeValidationId: Either[Result, String]): Interpretation[Either[Result, Result]] =
    maybeValidationId match {
      case Right(validationId) => Right(Redirect(s"$completionUrl?validationId=$validationId"))
      case Left(badRequest) => Left(badRequest)
    }

  private implicit def pure[T](value: T): Interpretation[T] =
    implicitly[Monad[Interpretation]].pure(value)
}
