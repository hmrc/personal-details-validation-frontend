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
import play.api.libs.json.Json
import play.api.test.Helpers._
import setups.connectors.HttpClientStubSetup
import uk.gov.hmrc.errorhandling.ProcessingError
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.personaldetailsvalidation.generators.ObjectGenerators._
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators._
import uk.gov.hmrc.personaldetailsvalidation.model.{FailedPersonalDetailsValidation, SuccessfulPersonalDetailsValidation}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.{global => executionContext}

class FuturedPersonalDetailsSenderSpec
  extends UnitSpec
    with MockFactory
    with ScalaFutures {

  "submitValidationRequest" should {

    "returned SuccessfulPersonalDetailsValidation from POST to /personal-details-validation with nino" in new Setup {

      val validationId = validationIds.generateOne
      expectPost(toUrl = "http://host/personal-details-validation")
        .withPayload(payloadWithNino)
        .returning(status = CREATED, Json.obj(
          "validationStatus" -> "success",
          "validationId" -> validationId.value
        ))

      connector.submitValidationRequest(personalDetailsWithNino).value.futureValue shouldBe Right(SuccessfulPersonalDetailsValidation(validationId))
    }

    "returned FailedPersonalDetailsValidation from POST to /personal-details-validation with nino" in new Setup {

      expectPost(toUrl = "http://host/personal-details-validation")
        .withPayload(payloadWithNino)
        .returning(status = CREATED, Json.obj(
          "validationStatus" -> "failure",
          "validationId" -> validationIds.generateOne.value
        ))

      connector.submitValidationRequest(personalDetailsWithNino).value.futureValue shouldBe Right(FailedPersonalDetailsValidation)
    }

    Set(OK, NO_CONTENT, NOT_FOUND, INTERNAL_SERVER_ERROR) foreach { unexpectedStatus =>

      s"return a ProcessingError when POST to /personal-details-validation/ returns $unexpectedStatus" in new Setup {

        expectPost(toUrl = "http://host/personal-details-validation")
          .withPayload(payloadWithNino)
          .returning(unexpectedStatus, "some response body")

        connector.submitValidationRequest(personalDetailsWithNino).value.futureValue shouldBe Left(ProcessingError(
          s"Unexpected response from POST http://host/personal-details-validation with status: '$unexpectedStatus' and body: some response body"
        ))
      }
    }

    "return a ProcessingError when POST to /personal-details-validation throws an exception" in new Setup {

      val exception = new RuntimeException("message")
      expectPost(toUrl = "http://host/personal-details-validation")
        .withPayload(payloadWithNino)
        .throwing(exception)

      connector.submitValidationRequest(personalDetailsWithNino).value.futureValue shouldBe Left(ProcessingError(
        s"Call to POST http://host/personal-details-validation threw: $exception"
      ))
    }

    "returned SuccessfulPersonalDetailsValidation from POST to /personal-details-validation with postcode" in new Setup {

      val validationId = validationIds.generateOne
      expectPost(toUrl = "http://host/personal-details-validation")
        .withPayload(payloadWithPostcode)
        .returning(status = CREATED, Json.obj(
          "validationStatus" -> "success",
          "validationId" -> validationId.value
        ))

      connector.submitValidationRequest(personalDetailsWithPostcode).value.futureValue shouldBe Right(SuccessfulPersonalDetailsValidation(validationId))
    }

    "returned FailedPersonalDetailsValidation from POST to /personal-details-validation with postcode" in new Setup {

      val validationId = validationIds.generateOne
      expectPost(toUrl = "http://host/personal-details-validation")
        .withPayload(payloadWithPostcode)
        .returning(status = CREATED, Json.obj(
          "validationStatus" -> "failure",
          "validationId" -> validationId.value
        ))

      connector.submitValidationRequest(personalDetailsWithPostcode).value.futureValue shouldBe Right(FailedPersonalDetailsValidation)
    }

    Set(OK, NO_CONTENT, NOT_FOUND, INTERNAL_SERVER_ERROR) foreach { unexpectedStatus =>

      s"return a ProcessingError when POST with postcode to /personal-details-validation/ returns $unexpectedStatus" in new Setup {

        expectPost(toUrl = "http://host/personal-details-validation")
          .withPayload(payloadWithPostcode)
          .returning(unexpectedStatus, "some response body")

        connector.submitValidationRequest(personalDetailsWithPostcode).value.futureValue shouldBe Left(ProcessingError(
          s"Unexpected response from POST http://host/personal-details-validation with status: '$unexpectedStatus' and body: some response body"
        ))
      }
    }

    "return a ProcessingError when POST with postcode to /personal-details-validation throws an exception" in new Setup {

      val exception = new RuntimeException("message")
      expectPost(toUrl = "http://host/personal-details-validation")
        .withPayload(payloadWithPostcode)
        .throwing(exception)

      connector.submitValidationRequest(personalDetailsWithPostcode).value.futureValue shouldBe Left(ProcessingError(
        s"Call to POST http://host/personal-details-validation threw: $exception"
      ))
    }
  }

  private trait Setup extends HttpClientStubSetup {
    implicit val headerCarrier = HeaderCarrier()

    val personalDetailsWithNino = personalDetailsObjects.generateOne

    val payloadWithNino = Json.obj(
      "firstName" -> personalDetailsWithNino.firstName.toString(),
      "lastName" -> personalDetailsWithNino.lastName.toString(),
      "dateOfBirth" -> personalDetailsWithNino.dateOfBirth,
      "nino" -> personalDetailsWithNino.nino.toString()
    )

    val personalDetailsWithPostcode = personalDetailsObjectsWithPostcode.generateOne

    val payloadWithPostcode = Json.obj(
      "firstName" -> personalDetailsWithPostcode.firstName.toString(),
      "lastName" -> personalDetailsWithPostcode.lastName.toString(),
      "dateOfBirth" -> personalDetailsWithPostcode.dateOfBirth,
      "postCode" -> personalDetailsWithPostcode.postCode.toString()
    )

    private val connectorConfig = new ConnectorConfig(mock[Configuration]) {
      override lazy val personalDetailsValidationBaseUrl = "http://host"
    }

    val connector = new FuturedPersonalDetailsSender(httpClient, connectorConfig)
  }
}
