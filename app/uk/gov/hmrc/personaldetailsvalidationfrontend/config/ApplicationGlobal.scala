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

import play.api.{Application, Configuration}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Request
import uk.gov.hmrc.personaldetailsvalidationfrontend.views.html.error_template
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode}
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.frontend.config.LoadAuditingConfig
import uk.gov.hmrc.play.frontend.filters.{FrontendAuditFilter, FrontendLoggingFilter, MicroserviceFilterSupport}

object ApplicationGlobal extends DefaultFrontendGlobal with RunMode {

  object AuditConnector extends AuditConnector with AppName {
    override lazy val auditingConfig = LoadAuditingConfig(s"$env.auditing")
  }

  object ControllerConfiguration extends ControllerConfig {
    override lazy val controllerConfigs = FrontendAppConfig.controllersConfiguration
  }

  object LoggingFilter extends FrontendLoggingFilter with MicroserviceFilterSupport {
    override def controllerNeedsLogging(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsLogging
  }

  object FrontendAuditFilter extends FrontendAuditFilter with AppName with MicroserviceFilterSupport {

    override lazy val maskedFormFields = Seq("password")

    override lazy val applicationPort = None

    override lazy val auditConnector = AuditConnector

    override def controllerNeedsAuditing(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsAuditing
  }

  override def auditConnector = AuditConnector

  override def loggingFilter = LoggingFilter

  override def frontendAuditFilter = FrontendAuditFilter

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = FrontendAppConfig.metricsConfiguration

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]) = error_template(pageTitle, heading, message)
}
