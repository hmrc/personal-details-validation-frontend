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

import cats.data.EitherT
import javax.inject.{Inject, Singleton}
import play.api.http.Status.CREATED
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.errorhandling.ProcessingError
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}
import uk.gov.hmrc.personaldetailsvalidation.model._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

private[personaldetailsvalidation] trait PersonalDetailsSender[Interpretation[_]] {

  def submitValidationRequest(personalDetails: PersonalDetails, origin: String)
                             (implicit headerCarrier: HeaderCarrier, executionContext: ExecutionContext)
  : EitherT[Interpretation, ProcessingError, PersonalDetailsValidation]
}

@Singleton
private[personaldetailsvalidation] class FuturedPersonalDetailsSender @Inject()(
                                                   httpClient: HttpClient,
                                                   connectorConfig: ConnectorConfig)
  extends PersonalDetailsSender[Future] {

  import connectorConfig.personalDetailsValidationBaseUrl

  private val url = s"$personalDetailsValidationBaseUrl/personal-details-validation"

  override def submitValidationRequest(personalDetails: PersonalDetails, origin: String)
                                      (implicit headerCarrier: HeaderCarrier, executionContext: ExecutionContext)
  : EitherT[Future, ProcessingError, PersonalDetailsValidation] =
    EitherT {
      httpClient.POST(url, body = Json.toJson(personalDetails)(personalDetailsWrites), headers = List(("origin", origin)))
        .recover(toProcessingError)
    }

  private implicit val personalDetailsSubmissionReads: HttpReads[Either[ProcessingError, PersonalDetailsValidation]] =
    (method: String, url: String, response: HttpResponse) => response.status match {
      case CREATED => Right(response.json.as[PersonalDetailsValidation])
      case other => Left(ProcessingError(s"Unexpected response from $method $url with status: '$other' and body: ${response.body}"))
    }

  private implicit val personalDetailsWrites: Writes[PersonalDetails] = Writes[PersonalDetails] {
    case personalDetails: PersonalDetailsWithNino =>
      Json.obj(
        "firstName" -> personalDetails.firstName,
        "lastName" -> personalDetails.lastName,
        "dateOfBirth" -> personalDetails.dateOfBirth,
        "nino" -> personalDetails.nino
      )
    case personalDetails: PersonalDetailsWithPostcode =>
      Json.obj(
        "firstName" -> personalDetails.firstName,
        "lastName" -> personalDetails.lastName,
        "dateOfBirth" -> personalDetails.dateOfBirth,
        "postCode" -> personalDetails.postCode
      )
  }

  private val toProcessingError: PartialFunction[Throwable, Either[ProcessingError, PersonalDetailsValidation]] = {
    case exception => Left(ProcessingError(s"Call to POST $url threw: $exception"))
  }
}
