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

import play.api.Play.current
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.config.DwpMessagesApi
import play.api.i18n.Messages.Implicits.applicationMessagesApi

trait DwpI18nSupport
  extends I18nSupport {

  def dwpMessagesApi: DwpMessagesApi

  lazy val messagesApi = applicationMessagesApi

  override implicit def request2Messages(implicit request: RequestHeader): Messages = {
    request.session.get("loginOrigin") match {
      case Some("dwp-iv") => dwpMessagesApi.preferred(request)
      case _ => messagesApi.preferred(request)
    }
  }
}
