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

package uk.gov.hmrc.personaldetailsvalidation.monitoring.analytics

import play.api.Logging
import play.api.mvc.Request
import uk.gov.hmrc.config.AppConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.personaldetailsvalidation.monitoring._

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AnalyticsEventHandler @Inject()(appConfig: AppConfig, connector: AnalyticsConnector)
  extends EventHandler with Logging with AnalyticsRequestFactory{

  override def handleEvent(event: MonitoringEvent)(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Unit = {
    event match {
      case e:BeginPDV => sendEvent(serviceBegin)
      case e:TimeoutContinue => sendEvent(timeoutContinue)
      case e:TimedOut => sendEvent(timeoutSignOut)
      case e:SignedOut => sendEvent(signedOut)
      case e:UnderNinoAge => sendEvent(underNinoAge)
      case e:PdvFailedAttempt => sendEvent(pdvFailedAttempt(e.attempts))
      case e:PdvLockedOut => sendEvent(pdvLockedOut)
      case e:PdvRetry => sendEvent(pdvRetry(e.retryGuidanceText))
      case e:PDVServiceUnavailable => sendEvent(serviceUnavailable)
      case _ => ()
    }
  }

  private def sendEvent(reqCreator: (Option[String], Seq[DimensionValue]) => AnalyticsRequest)
                       (implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Unit = {

    val gaOriginDimension: Int = appConfig.originDimension
    val origin: String = request.session.get("origin").getOrElse("unknown")
    val dimensions: Seq[DimensionValue] = Seq(DimensionValue(gaOriginDimension, origin))

    val gaClientId: Option[String] = request.cookies.get("_ga").map(_.value)

    if (gaClientId.isEmpty) {
      logger.info("Unable to use users' cookies- No gaClientId found in request")
    }

    val analyticsRequest = reqCreator(gaClientId, dimensions)
    connector.sendEvent(analyticsRequest)
  }
}

trait AnalyticsRequestFactory {

  def serviceBegin(clientId: Option[String], dimensions: Seq[DimensionValue]): AnalyticsRequest = {
    val gaEvent = Event("sos_iv", "personal_detail_validation_result", "begin", dimensions)
    AnalyticsRequest(clientId, Seq(gaEvent))
  }

  def serviceUnavailable(clientId: Option[String], dimensions: Seq[DimensionValue]): AnalyticsRequest = {
    val gaEvent = Event("sos_iv","personal_detail_validation_result",s"pdv_service_unavailable",dimensions)
    AnalyticsRequest(clientId, Seq(gaEvent))
  }

  def timeoutContinue(clientId: Option[String], dimensions: Seq[DimensionValue]): AnalyticsRequest = {
    val gaEvent = Event("sos_iv", "pdv_modal_timeout", "continue", dimensions)
    AnalyticsRequest(clientId, Seq(gaEvent))
  }

  def timeoutSignOut(clientId: Option[String], dimensions: Seq[DimensionValue]): AnalyticsRequest = {
    val gaEvent = Event("sos_iv", "pdv_modal_timeout", "end", dimensions)
    AnalyticsRequest(clientId, Seq(gaEvent))
  }

  def signedOut(clientId: Option[String], dimensions: Seq[DimensionValue]): AnalyticsRequest = {
    val gaEvent = Event("sos_iv", "personal_detail_validation_result", "sign_out_pdv", dimensions)
    AnalyticsRequest(clientId, Seq(gaEvent))
  }

  def underNinoAge(clientId: Option[String], dimensions: Seq[DimensionValue]): AnalyticsRequest = {
    val gaEvent = Event("sos_iv", "personal_detail_validation_result", "under_nino_age", dimensions)
    AnalyticsRequest(clientId, Seq(gaEvent))
  }

  def pdvFailedAttempt(attemptsLeft: Int)(clientId: Option[String], dimensions: Seq[DimensionValue]): AnalyticsRequest = {
    val gaEvent = Event("sos_iv","pdv_locking",s"pdv_fail$attemptsLeft",dimensions)
    AnalyticsRequest(clientId, Seq(gaEvent))
  }

  def pdvLockedOut(clientId: Option[String], dimensions: Seq[DimensionValue]): AnalyticsRequest = {
    val gaEvent = Event("sos_iv","pdv_locking","pdv_locked-out",dimensions)
    AnalyticsRequest(clientId, Seq(gaEvent))
  }

  def pdvRetry(retryGuidanceText: String)(clientId: Option[String], dimensions: Seq[DimensionValue]): AnalyticsRequest = {
    val gaEvent = Event("sos_iv","pdv_locking",s"${retryGuidanceText}_retry",dimensions)
    AnalyticsRequest(clientId, Seq(gaEvent))
  }

}
