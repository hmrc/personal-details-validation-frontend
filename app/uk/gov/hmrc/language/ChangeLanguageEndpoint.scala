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

import javax.inject.{Inject, Singleton}
import play.api.i18n.Lang
import play.api.mvc.{Action, AnyContent, Controller}
import uk.gov.hmrc.config.{AppConfig, DwpMessagesApi}
import uk.gov.hmrc.errorhandling.ErrorHandler
import uk.gov.hmrc.play.language.LanguageUtils.FlashWithSwitchIndicator
import uk.gov.hmrc.language.DwpI18nSupport

import scala.language.implicitConversions

@Singleton
class ChangeLanguageEndpoint @Inject()(config: LanguagesConfig,
                                       errorHandler: ErrorHandler,
                                       appConfig: AppConfig)(implicit val dwpMessagesApi: DwpMessagesApi)
  extends DwpI18nSupport(appConfig)
    with Controller {

  def switchTo(language: String): Action[AnyContent] = Action { implicit request =>
    request.headers.get(REFERER) match {
      case Some(redirectUrl) =>
        Redirect(redirectUrl)
          .withLang(language)
          .flashing(FlashWithSwitchIndicator)
      case None =>
        BadRequest(errorHandler.internalServerErrorTemplate)
    }
  }

  private implicit def toLang(langCode: String): Lang =
    config.languagesMap.getOrElse(langCode, Lang.defaultLang)
}
