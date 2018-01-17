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

import javax.inject.Singleton

import cats.Monad
import cats.implicits._
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.personaldetailsvalidation.model.CompletionUrl

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.{higherKinds, implicitConversions}

@Singleton
private class FuturedJourneyStart extends JourneyStart[Future]()

private class JourneyStart[Interpretation[_] : Monad]() {

  def findRedirect(completionUrl: CompletionUrl): Interpretation[Result] =
    Redirect(routes.PersonalDetailsCollectionController.showPage(completionUrl))

  private implicit def pure[R](value: R): Interpretation[R] =
    implicitly[Monad[Interpretation]].pure(value)

}
