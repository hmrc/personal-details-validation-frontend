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

import javax.inject.{Inject, Singleton}

import play.api.http.Status.{NOT_FOUND, OK}
import uk.gov.hmrc.http.{BadGatewayException, HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

private[personaldetailsvalidation] trait ValidationIdValidator[Interpretation[_]] {
  def verify(validationId: String)
            (implicit headerCarrier: HeaderCarrier, executionContext: ExecutionContext): Interpretation[Boolean]
}

@Singleton
private[personaldetailsvalidation] class FuturedValidationIdValidator @Inject()(httpClient: HttpClient,
                                                                                connectorConfig: ConnectorConfig)
  extends ValidationIdValidator[Future] {

  import connectorConfig.personalDetailsValidationBaseUrl

  override def verify(validationId: String)
                     (implicit headerCarrier: HeaderCarrier, executionContext: ExecutionContext): Future[Boolean] =
    httpClient.GET(s"$personalDetailsValidationBaseUrl/personal-details-validation/$validationId")

  private implicit val validationIdHttpReads: HttpReads[Boolean] = new HttpReads[Boolean] {
    override def read(method: String, url: String, response: HttpResponse): Boolean = response.status match {
      case OK => true
      case NOT_FOUND => false
      case other =>
        throw new BadGatewayException(s"Unexpected response from $method $url with status: '$other' and body: ${response.body}")
    }
  }
}