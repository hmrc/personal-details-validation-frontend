/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.views

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.Call
import uk.gov.hmrc.config.DwpMessagesApiProvider
import uk.gov.hmrc.config.implicits._
import uk.gov.hmrc.config.ops._
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.language.{Cy, En, Language}
import uk.gov.hmrc.language.routes

@Singleton
class ViewConfig @Inject()(val configuration: Configuration,
                           protected val dwpMessagesApiProvider: DwpMessagesApiProvider)
{
  lazy val analyticsToken: String = configuration.loadMandatory("google-analytics.token")
  lazy val analyticsHost: String = configuration.loadMandatory("google-analytics.host")
  lazy val originDwp: String = configuration.getOptional[String]("dwp.originLabel").getOrElse("dwp-iv")
  lazy val dwpGetHelpUrl: String = configuration.loadMandatory("dwp.getHelpUrl")
  val timeout: Int = configuration.get[Int]("timeoutDialog.timeout-seconds")
  val timeoutCountdown: Int = configuration.get[Int]("timeoutDialog.timeout-countdown-seconds")

  def languageMap: Map[String, Lang] =
    configuration.load[Seq[String]]("play.i18n.langs", default = Nil)
      .map(verifyMessagesExists)
      .map(toLangNameAndLangTuples)
      .toMap

  private def toLangNameAndLangTuples(code: String): (String, Lang) =
    configuration.loadMandatory[String](s"play.i18n.descriptions.$code") -> Lang(code)

  private def verifyMessagesExists(code: String): String = {
    val validatedCode = if (code == "en") "default" else code
    dwpMessagesApiProvider.get.messages.keySet.find(_ == validatedCode) match {
      case Some(_) => code
      case None => throw new RuntimeException(s"No messages.$code defined")
    }
  }

  // old templates
  def routeToSwitchLanguage: String => Call =
    (lang: String) => routes.ChangeLanguageEndpoint.switchToLanguage(lang)

  // new templates
  def languageLinks: Seq[(Language, String)] = {
    Seq(
      (En, routes.ChangeLanguageEndpoint.switchToLanguage("english").url),
      (Cy, routes.ChangeLanguageEndpoint.switchToLanguage("cymraeg").url)
    )
  }

}
