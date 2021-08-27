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

package uk.gov.hmrc.personaldetailsvalidation.connectors

import javax.inject.{Inject, Singleton}
import play.api.Logging
import uk.gov.hmrc.config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.personaldetailsvalidation.model.JourneyUpdate

import scala.concurrent.ExecutionContext

@Singleton
class IdentityVerificationConnector @Inject()(appConfig: AppConfig,
                                              httpClient: HttpClient) extends Logging {

  def updateJourney(redirectingUrl: String)(implicit hc: HeaderCarrier, ec: ExecutionContext) = {

    val journeyId = extractJourneyId(redirectingUrl)
    val status = JourneyUpdate(Some("Timeout"))
    if (journeyId.isDefined) {
      httpClient.PATCH(s"${appConfig.ivUrl}/identity-verification/journey/${journeyId.get}", status)
    }
    else
      logger.warn(s"VER-333- cannot extract IV journeyId from redirecting url : = $redirectingUrl")
  }

  def extractJourneyId(url: String): Option[String] = url.split('/') match {
    //IV redirectURL always follows pattern /mdtp/personal-details-validation-complete/261948fb-b807-4e5a-a5ca-3cdcc5009be4
    case Array(_, _, _, journeyId) => Some(journeyId)
    case _ => None
  }

}
