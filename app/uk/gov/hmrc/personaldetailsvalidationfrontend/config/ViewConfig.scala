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

package uk.gov.hmrc.personaldetailsvalidationfrontend.config

import javax.inject.Inject

import play.api.Configuration
import uk.gov.hmrc.play.config.{AssetsConfig, OptimizelyConfig}

class ViewConfig @Inject()(protected val configuration: Configuration)
  extends AssetsConfig
    with OptimizelyConfig
    with BaseConfig {

  override lazy val assetsUrl: String = configuration.loadMandatory("assets.url")
  override lazy val assetsVersion: String = configuration.loadMandatory("assets.version")

  override lazy val optimizelyBaseUrl: String = configuration.load("optimizely.url", "")
  override lazy val optimizelyProjectId: Option[String] = configuration.loadOptional("optimizely.projectId")

  private lazy val contactHost = configuration.loadMandatory[Host]("contact-frontend")
  private lazy val contactFormServiceIdentifier = "personal-details-validation-frontend"
  lazy val reportAProblemPartialUrl: String = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl: String = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  lazy val analyticsToken: String = configuration.loadMandatory("google-analytics.token")
  lazy val analyticsHost: String = configuration.loadMandatory("google-analytics.host")
}
