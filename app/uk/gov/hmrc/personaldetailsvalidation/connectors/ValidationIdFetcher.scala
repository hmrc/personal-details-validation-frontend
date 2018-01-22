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
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.errorhandling.ProcessingError
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

private[personaldetailsvalidation] trait ValidationIdFetcher[Interpretation[_]] {

  def fetchValidationId(endpointUri: URI)
                       (implicit headerCarrier: HeaderCarrier,
                        executionContext: ExecutionContext): EitherT[Interpretation, ProcessingError, String]
}

@Singleton
private[personaldetailsvalidation] class FuturedValidationIdFetcher @Inject()(httpClient: HttpClient,
                                                                              connectorConfig: ConnectorConfig)
  extends ValidationIdFetcher[Future] {

  import connectorConfig.personalDetailsValidationBaseUrl

  override def fetchValidationId(endpointUri: URI)
                                (implicit headerCarrier: HeaderCarrier,
                                 executionContext: ExecutionContext): EitherT[Future, ProcessingError, String] = EitherT {
    val url = s"$personalDetailsValidationBaseUrl$endpointUri"

    httpClient
      .GET(url)
      .recover(toProcessingError(url))
  }

  private implicit val validationIdHttpReads: HttpReads[Either[ProcessingError, String]] = new HttpReads[Either[ProcessingError, String]] {
    override def read(method: String, url: String, response: HttpResponse): Either[ProcessingError, String] = response.status match {
      case OK => (response.json \ "id").validate[String] match {
        case JsSuccess(validationId, _) => Right(validationId)
        case JsError(_) => Left(ProcessingError(s"No 'id' property in the json response from $method $url"))
      }
      case other =>
        Left(ProcessingError(s"Unexpected response from $method $url with status: '$other' and body: ${response.body}"))
    }
  }

  private def toProcessingError(url: String): PartialFunction[Throwable, Either[ProcessingError, String]] = {
    case exception => Left(ProcessingError(s"Call to GET $url threw: $exception"))
  }
}
