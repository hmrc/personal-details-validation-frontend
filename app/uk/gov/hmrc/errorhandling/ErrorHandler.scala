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

package uk.gov.hmrc.errorhandling

import javax.inject.{Inject, Singleton}

import play.api.i18n.MessagesApi
import play.api.mvc.Results.NotFound
import play.api.mvc.{Request, RequestHeader, Result, Results}
import play.mvc.Http.Status._
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.http.FrontendErrorHandler
import uk.gov.hmrc.views.ViewConfig
import uk.gov.hmrc.views.html.template.error_template

import scala.concurrent.Future
import scala.language.implicitConversions

@Singleton
class ErrorHandler @Inject()()(implicit val messagesApi: MessagesApi,
                               viewConfig: ViewConfig)
  extends FrontendErrorHandler {

  import ErrorHandler.bindingError

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)
                                    (implicit request: Request[_]): Html =
    error_template(pageTitle, heading, message)

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    statusCode match {
      case BAD_REQUEST if message.startsWith(bindingError) => Future.successful(NotFound(internalServerErrorTemplate(request)))
      case other => Future.successful(Results.Status(other)(internalServerErrorTemplate(request)))
    }
  }

  private implicit def rhToRequest(rh: RequestHeader): Request[_] = Request(rh, "")
}

object ErrorHandler {
  val bindingError: String = "binding-error: "
}