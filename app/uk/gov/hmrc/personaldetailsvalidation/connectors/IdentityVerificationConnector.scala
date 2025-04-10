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

package uk.gov.hmrc.personaldetailsvalidation.connectors

import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.config.AppConfig
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.personaldetailsvalidation.model.JourneyUpdate.format
import uk.gov.hmrc.personaldetailsvalidation.model.JourneyUpdate

import scala.concurrent.ExecutionContext

@Singleton
class IdentityVerificationConnector @Inject()(appConfig: AppConfig,
                                              httpClient: HttpClientV2) extends Logging {

  val mdtpUrl = "/mdtp/personal-details-validation-complete/"

  def updateJourney(redirectingUrl: String, journeyStatus: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Any = {

    val journeyId = extractJourneyId(redirectingUrl)
    val status = JourneyUpdate(Some(journeyStatus))

    if (journeyId.isDefined) {

      httpClient.patch(url"${appConfig.ivUrl}/identity-verification/journey/${journeyId.getOrElse("")}").withBody(Json.toJson(status)(format)).execute[HttpResponse]
        .recover {
        case ex: Exception => logger.warn(s"IV returns error ${ex.getMessage}, update IV journey might failed for ${journeyId.getOrElse("")}")
      }

    }
    else
      logger.warn(s"Cannot extract IV journeyId from redirecting url: $redirectingUrl")
  }

  def extractJourneyId(url: String): Option[String] = {
    if (url.contains(mdtpUrl)) {
      Some(url.split(mdtpUrl).last)
    } else None
  }

}
