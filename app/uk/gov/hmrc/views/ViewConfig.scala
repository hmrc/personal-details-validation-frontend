/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.Configuration
import play.api.i18n.Lang
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.config.DwpMessagesApiProvider
import uk.gov.hmrc.config.implicits._
import uk.gov.hmrc.config.ops._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ViewConfig @Inject()(val configuration: Configuration,
                           servicesConfig: ServicesConfig,
                           protected val dwpMessagesApiProvider: DwpMessagesApiProvider,
                           val authConnector: AuthConnector) extends AuthorisedFunctions
{
  lazy val retryLimit: Int = configuration.getOptional[Int]("retry.limit").getOrElse(5)

  lazy val analyticsToken: String = configuration.loadMandatory("google-analytics.token")
  lazy val analyticsHost: String = configuration.loadMandatory("google-analytics.host")
  lazy val originDwp: String = configuration.getOptional[String]("dwp.originLabel").getOrElse("dwp-iv")
  lazy val dwpGetHelpUrl: String = configuration.loadMandatory("dwp.getHelpUrl")
  lazy val timeout: Int = configuration.get[Int]("timeoutDialog.timeout-seconds")
  lazy val timeoutCountdown: Int = configuration.get[Int]("timeoutDialog.timeout-countdown-seconds")
  lazy val lockoutPeriodEn: String = configuration.getOptional[String]("lockout.period.en").getOrElse("24 hours")
  lazy val lockoutPeriodCy: String = configuration.getOptional[String]("lockout.period.cy").getOrElse("24 awr")
  lazy val isLocal: Boolean = configuration.getOptional[Boolean]("isLocal").getOrElse(false)

  def addTaxesFrontendBaseUrl(): String =
    if (isLocal) servicesConfig.baseUrl("add-taxes-frontend") else ""

  def languageMap: Map[String, Lang] =
    configuration.load[Seq[String]]("play.i18n.langs", default = Seq("en", "cy"))
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

  def isLoggedIn(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    authorised(){
      Future.successful(true)
    }.recover{
      case _ => false
    }
  }

}
