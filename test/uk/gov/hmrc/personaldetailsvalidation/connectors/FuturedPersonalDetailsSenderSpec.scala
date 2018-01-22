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
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.Configuration
import play.api.http.HeaderNames.LOCATION
import play.api.libs.json.Json
import play.api.test.Helpers._
import setups.connectors.HttpClientStubSetup
import uk.gov.hmrc.errorhandling.ProcessingError
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.personaldetailsvalidation.generators.ObjectGenerators.personalDetailsObjects
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.{global => executionContext}

class FuturedPersonalDetailsSenderSpec
  extends UnitSpec
    with MockFactory
    with ScalaFutures {

  "passToValidation" should {

    "pass returned Location from POST to /personal-details-validation" in new Setup {

      val locationUri = uris.generateOne
      expectPost(toUrl = "http://host/personal-details-validation")
        .withPayload(payload)
        .returning(status = CREATED, headers = LOCATION -> locationUri.toString)

      connector.passToValidation(personalDetails).value.futureValue shouldBe Right(locationUri)
    }

    s"return a ProcessingError when there is no $LOCATION header " +
      "in the response from POST to /personal-details-validation" in new Setup {

      expectPost(toUrl = "http://host/personal-details-validation")
        .withPayload(payload)
        .returning(status = CREATED)

      connector.passToValidation(personalDetails).value.futureValue shouldBe Left(ProcessingError(
        "No Location header in the response from POST http://host/personal-details-validation"
      ))
    }

    Set(OK, NO_CONTENT, NOT_FOUND, INTERNAL_SERVER_ERROR) foreach { unexpectedStatus =>

      s"return a ProcessingError when POST to /personal-details-validation/ returns $unexpectedStatus" in new Setup {

        expectPost(toUrl = "http://host/personal-details-validation")
          .withPayload(payload)
          .returning(unexpectedStatus, "some response body")

        connector.passToValidation(personalDetails).value.futureValue shouldBe Left(ProcessingError(
          s"Unexpected response from POST http://host/personal-details-validation with status: '$unexpectedStatus' and body: some response body"
        ))
      }
    }

    "return a ProcessingError when POST to /personal-details-validation throws an exception" in new Setup {

      val exception = new RuntimeException("message")
      expectPost(toUrl = "http://host/personal-details-validation")
        .withPayload(payload)
        .throwing(exception)

      connector.passToValidation(personalDetails).value.futureValue shouldBe Left(ProcessingError(
        s"Call to POST http://host/personal-details-validation threw: $exception"
      ))
    }
  }

  private trait Setup extends HttpClientStubSetup {
    implicit val headerCarrier = HeaderCarrier()

    val personalDetails = personalDetailsObjects.generateOne

    val payload = Json.obj(
      "firstName" -> personalDetails.firstName.toString(),
      "lastName" -> personalDetails.lastName.toString(),
      "dateOfBirth" -> personalDetails.dateOfBirth,
      "nino" -> personalDetails.nino.toString()
    )

    private val connectorConfig = new ConnectorConfig(mock[Configuration]) {
      override lazy val personalDetailsValidationBaseUrl = "http://host"
    }

    val connector = new FuturedPersonalDetailsSender(httpClient, connectorConfig)
  }
}
