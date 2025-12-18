/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.i18n.MessagesApi
import play.api.mvc.Results.NotFound
import play.api.mvc.{RequestHeader, Result, Results}
import play.mvc.Http.Status.*
import play.twirl.api.Html
import uk.gov.hmrc.config.{AppConfig, DwpMessagesApiProvider}
import uk.gov.hmrc.language.DwpI18nSupport
import uk.gov.hmrc.views.ViewConfig
import uk.gov.hmrc.views.html.template.error_template

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ErrorHandler @Inject()(appConfig: AppConfig, error_template: error_template)
                            (implicit val dwpMessagesApiProvider: DwpMessagesApiProvider, val ec: ExecutionContext, viewConfig: ViewConfig)
  extends DwpI18nSupport(appConfig, dwpMessagesApiProvider.get()) {

  import ErrorHandler.bindingError

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: RequestHeader): Future[Html] = {
    Future.successful(error_template(pageTitle, heading, message))
  }

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    statusCode match {
      case BAD_REQUEST if message.startsWith(bindingError) => internalServerErrorTemplate(using request).map{ html => NotFound(html) }
      case other => internalServerErrorTemplate(using request).map{ html => Results.Status(other)(html) }
    }
  }
  
  override def messagesApi: MessagesApi = dwpMessagesApiProvider.get
}

object ErrorHandler {
  val bindingError: String = "binding-error: "
}