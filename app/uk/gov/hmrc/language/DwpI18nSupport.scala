/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.language

import com.google.inject.Inject
import play.api.Play.current
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Request, RequestHeader}
import uk.gov.hmrc.config.{AppConfig, DwpMessagesApiProvider}
import play.api.i18n.Messages.Implicits.applicationMessagesApi
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.http.FrontendErrorHandler

abstract class DwpI18nSupport @Inject()(appConfig: AppConfig)
  extends FrontendErrorHandler with I18nSupport {

  def dwpMessagesApiProvider: DwpMessagesApiProvider

  lazy val messagesApi: MessagesApi = implicitly[MessagesApi]

  override implicit def request2Messages(implicit request: RequestHeader): Messages = {
    request.session.get("loginOrigin") match {
      case Some(appConfig.originDwp) => dwpMessagesApiProvider.get.preferred(request)
      case _ => messagesApi.preferred(request)
    }
  }

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html  = ???
}

