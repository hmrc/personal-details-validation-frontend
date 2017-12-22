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

package uk.gov.hmrc.personaldetailsvalidation.connectors

import java.net.URI
import javax.inject.{Inject, Singleton}

import play.api.http.HeaderNames._
import play.api.http.Status.CREATED
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.{BadGatewayException, HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.personaldetailsvalidation.model.PersonalDetails
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

private[personaldetailsvalidation] trait PersonalDetailsSender[Interpretation[_]] {

  def passToValidation(personalDetails: PersonalDetails)
                      (implicit headerCarrier: HeaderCarrier,
                       executionContext: ExecutionContext): Interpretation[URI]
}

@Singleton
private[personaldetailsvalidation] class FuturedPersonalDetailsSender @Inject()(httpClient: HttpClient,
                                                                                connectorConfig: ConnectorConfig)
  extends PersonalDetailsSender[Future] {

  import connectorConfig.personalDetailsValidationBaseUrl

  override def passToValidation(personalDetails: PersonalDetails)
                               (implicit headerCarrier: HeaderCarrier,
                                executionContext: ExecutionContext): Future[URI] =
    httpClient.POST(
      url = s"$personalDetailsValidationBaseUrl/personal-details-validation",
      body = personalDetails.toJson
    )

  private implicit val personalDetailsSubmissionReads: HttpReads[URI] = new HttpReads[URI] {
    override def read(method: String, url: String, response: HttpResponse): URI = response.status match {
      case CREATED => response.header(LOCATION).map(new URI(_)).getOrElse {
        throw new BadGatewayException(s"No $LOCATION header in the response from $method $url")
      }
      case other =>
        throw new BadGatewayException(s"Unexpected response from $method $url with status: '$other' and body: ${response.body}")
    }
  }

  private implicit class PersonalDetailsSerializer(personalDetails: PersonalDetails) {
    lazy val toJson: JsObject = Json.obj(
      "firstName" -> personalDetails.firstName,
      "lastName" -> personalDetails.lastName,
      "dateOfBirth" -> personalDetails.dateOfBirth,
      "nino" -> personalDetails.nino
    )
  }
}
