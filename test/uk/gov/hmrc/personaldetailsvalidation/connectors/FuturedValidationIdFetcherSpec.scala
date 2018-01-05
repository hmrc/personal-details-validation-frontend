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

import generators.Generators.Implicits._
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.Configuration
import play.api.libs.json.{JsNull, Json}
import play.api.test.Helpers._
import setups.connectors.HttpClientStubSetup
import uk.gov.hmrc.http.{BadGatewayException, HeaderCarrier}
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.{global => executionContext}

class FuturedValidationIdFetcherSpec
  extends UnitSpec
    with MockFactory
    with ScalaFutures {

  "fetchValidationId" should {

    "call GET on the given uri " +
      "and extract validationId from the response" in new Setup {

      val validationId = Gen.uuid.generateOne.toString

      expectGet(toUrl = s"$baseUrl$endpointUri")
        .returning(status = OK, body = Json.obj("id" -> validationId))

      connector.fetchValidationId(endpointUri).futureValue shouldBe validationId
    }

    s"throw a BadGatewayException when there is no 'id' " +
      "in the response from GET to the given uri" in new Setup {

      expectGet(toUrl = s"$baseUrl$endpointUri")
        .returning(status = OK, body = JsNull)

      val exception = intercept[BadGatewayException] {
        await(connector.fetchValidationId(endpointUri))
      }
      exception.message shouldBe s"No 'id' property in the json response from GET $baseUrl$endpointUri"
      exception.responseCode shouldBe BAD_GATEWAY
    }

    Set(NO_CONTENT, NOT_FOUND, INTERNAL_SERVER_ERROR) foreach { unexpectedStatus =>

      s"throw a BadGatewayException when GET to the given uri returns $unexpectedStatus" in new Setup {

        expectGet(toUrl = s"$baseUrl$endpointUri")
          .returning(unexpectedStatus, "some response body")

        val exception = intercept[BadGatewayException] {
          await(connector.fetchValidationId(endpointUri))
        }
        exception.message shouldBe s"Unexpected response from GET $baseUrl$endpointUri with status: '$unexpectedStatus' and body: some response body"
        exception.responseCode shouldBe BAD_GATEWAY
      }
    }
  }

  private trait Setup extends HttpClientStubSetup {
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

    val endpointUri = uris.generateOne

    val baseUrl = "http://host"
    private val connectorConfig = new ConnectorConfig(mock[Configuration]) {
      override lazy val personalDetailsValidationBaseUrl: String = baseUrl
    }

    val connector = new FuturedValidationIdFetcher(httpClient, connectorConfig)
  }
}