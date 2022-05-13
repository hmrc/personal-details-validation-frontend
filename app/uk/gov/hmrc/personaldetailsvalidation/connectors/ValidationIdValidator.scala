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

package uk.gov.hmrc.personaldetailsvalidation.connectors

import play.api.http.Status.{NOT_FOUND, OK}
import uk.gov.hmrc.errorhandling.ProcessingError
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}
import uk.gov.hmrc.personaldetailsvalidation.model.ValidationId

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ValidationIdValidator @Inject()(httpClient: HttpClient,connectorConfig: ConnectorConfig) {

  import connectorConfig.personalDetailsValidationBaseUrl

  def verify(validationId: ValidationId)
            (implicit headerCarrier: HeaderCarrier,
             executionContext: ExecutionContext): Future[Boolean] = {

    val url = s"$personalDetailsValidationBaseUrl/personal-details-validation/${validationId.value}"

    httpClient.GET[Boolean](url).recover(toProcessingError(url))
  }

  private implicit val validationIdHttpReads: HttpReads[Either[ProcessingError, Boolean]] = new HttpReads[Either[ProcessingError, Boolean]] {
    override def read(method: String, url: String, response: HttpResponse): Either[ProcessingError, Boolean] = response.status match {
      case OK => Right(true)
      case NOT_FOUND => Right(false)
      case other => Left(ProcessingError(s"Unexpected response from $method $url with status: '$other' and body: ${response.body}"))
    }
  }

  private def toProcessingError(url: String): PartialFunction[Throwable, Either[ProcessingError, Boolean]] = {
    case exception => Left(ProcessingError(s"Call to GET $url threw: $exception"))
  }

}