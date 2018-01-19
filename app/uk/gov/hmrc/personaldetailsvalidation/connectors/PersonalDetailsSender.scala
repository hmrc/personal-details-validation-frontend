/*
 * Copyright 2018 HM Revenue & Customs
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

import cats.data.EitherT
import play.api.http.HeaderNames._
import play.api.http.Status.CREATED
import play.api.libs.json.{Format, JsObject, Json}
import uk.gov.hmrc.errorhandling.ProcessingError
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.personaldetailsvalidation.model.{NonEmptyString, PersonalDetails}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.voa.valuetype.play.formats.ValueTypeFormat._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

private[personaldetailsvalidation] trait PersonalDetailsSender[Interpretation[_]] {

  def passToValidation(personalDetails: PersonalDetails)
                      (implicit headerCarrier: HeaderCarrier,
                       executionContext: ExecutionContext): EitherT[Interpretation, ProcessingError, URI]
}

@Singleton
private[personaldetailsvalidation] class FuturedPersonalDetailsSender @Inject()(httpClient: HttpClient,
                                                                                connectorConfig: ConnectorConfig)
  extends PersonalDetailsSender[Future] {

  import connectorConfig.personalDetailsValidationBaseUrl

  private val url = s"$personalDetailsValidationBaseUrl/personal-details-validation"

  override def passToValidation(personalDetails: PersonalDetails)
                               (implicit headerCarrier: HeaderCarrier,
                                executionContext: ExecutionContext): EitherT[Future, ProcessingError, URI] = EitherT {
    httpClient
      .POST(
        url,
        body = personalDetails.toJson
      ).recover(toProcessingError)
  }

  private implicit val personalDetailsSubmissionReads: HttpReads[Either[ProcessingError, URI]] = new HttpReads[Either[ProcessingError, URI]] {
    override def read(method: String, url: String, response: HttpResponse): Either[ProcessingError, URI] = response.status match {
      case CREATED => response.header(LOCATION) match {
        case Some(location) => Right(new URI(location))
        case None => Left(ProcessingError(s"No $LOCATION header in the response from $method $url"))
      }
      case other =>
        Left(ProcessingError(s"Unexpected response from $method $url with status: '$other' and body: ${response.body}"))
    }
  }

  private implicit val nonEmptyStringFormat: Format[NonEmptyString] = format(NonEmptyString.apply)

  private implicit class PersonalDetailsSerializer(personalDetails: PersonalDetails) {
    lazy val toJson: JsObject = Json.obj(
      "firstName" -> personalDetails.firstName,
      "lastName" -> personalDetails.lastName,
      "dateOfBirth" -> personalDetails.dateOfBirth,
      "nino" -> personalDetails.nino
    )
  }

  private val toProcessingError: PartialFunction[Throwable, Either[ProcessingError, URI]] = {
    case exception => Left(ProcessingError(s"Call to POST $url threw: $exception"))
  }
}
