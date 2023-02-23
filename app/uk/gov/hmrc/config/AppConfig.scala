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

package uk.gov.hmrc.config

import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject()(val configuration: Configuration, servicesConfig: ServicesConfig) {

  lazy val isLocal: Boolean = configuration.getOptional[Boolean]("isLocal").getOrElse(false)
  lazy val helplineUrl: String = if (isLocal) servicesConfig.baseUrl("helpline-frontend") else ""

  lazy val ivUrl: String = servicesConfig.baseUrl("identity-verification")
  lazy val originDwp: String = configuration.getOptional[String]("dwp.originLabel").getOrElse("dwp-iv")

  lazy val enabledCircuitBreaker: Boolean = configuration.getOptional[Boolean]("circuit-breaker.enabled").getOrElse(false)

  //Lockout
  lazy val retryLimit: Int = configuration.getOptional[Int]("retry.limit").getOrElse(5)

  //GA related configs
  lazy val platformAnalyticsUrl: String = servicesConfig.baseUrl("platform-analytics")
  def analyticsToken: String = configuration.getOptional[String]("google-analytics.token").getOrElse("")
  lazy val originDimension: Int = configuration.get[Int]("google-analytics.origin-dimension")

  //logout related configs
  lazy val logoutPage: String = servicesConfig.getConfString("logoutPage", "https://www.access.service.gov.uk/logout")
  lazy val basGatewayUrl: String = servicesConfig.getConfString("auth.bas-gateway.url", throw new RuntimeException("Bas gateway url required"))
  lazy val logoutPath: String = servicesConfig.getConfString("auth.logOutUrl", "")
  lazy val ggLogoutUrl = s"$basGatewayUrl$logoutPath"
  lazy val logoutCallback: String = servicesConfig.getConfString("auth.logoutCallbackUrl", "/personal-details-validation/signed-out")

}
