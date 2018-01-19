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
import generators.Generators.nonEmptyStrings
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.Configuration
import play.api.test.Helpers._
import setups.connectors.HttpClientStubSetup
import uk.gov.hmrc.errorhandling.ProcessingError
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.{global => executionContext}

class FuturedValidationIdValidatorSpec
  extends UnitSpec
    with MockFactory
    with ScalaFutures {

  "verify" should {

    "return true if call to GET /personal-details-validation/:validationId returns OK" in new Setup {

      expectGet(toUrl = s"$baseUrl/personal-details-validation/$validationId")
        .returning(status = OK)

      validationIdValidator.verify(validationId).value.futureValue shouldBe Right(true)
    }

    "return false if call to GET /personal-details-validation/:validationId returns NOT_FOUND" in new Setup {

      expectGet(toUrl = s"$baseUrl/personal-details-validation/$validationId")
        .returning(status = NOT_FOUND)

      validationIdValidator.verify(validationId).value.futureValue shouldBe Right(false)
    }

    Set(NO_CONTENT, BAD_REQUEST, INTERNAL_SERVER_ERROR) foreach { unexpectedStatus =>

      s"return a ProcessingError when GET /personal-details-validation/:validationId returns $unexpectedStatus" in new Setup {

        expectGet(toUrl = s"$baseUrl/personal-details-validation/$validationId")
          .returning(unexpectedStatus, "some response body")

        validationIdValidator.verify(validationId).value.futureValue shouldBe Left(ProcessingError(
          s"Unexpected response from GET $baseUrl/personal-details-validation/$validationId with status: '$unexpectedStatus' and body: some response body"
        ))
      }
    }

    "return a ProcessingError when GET /personal-details-validation/:validationId throws an exception" in new Setup {

      val exception = new RuntimeException("Some error")
      expectGet(toUrl = s"$baseUrl/personal-details-validation/$validationId")
        .throwing(exception)

      validationIdValidator.verify(validationId).value.futureValue shouldBe Left(ProcessingError(
        s"Call to GET $baseUrl/personal-details-validation/$validationId threw: $exception"
      ))
    }
  }

  private trait Setup extends HttpClientStubSetup {

    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

    val validationId = nonEmptyStrings.generateOne

    val baseUrl = "http://host"
    private val connectorConfig = new ConnectorConfig(mock[Configuration]) {
      override lazy val personalDetailsValidationBaseUrl: String = baseUrl
    }

    val validationIdValidator = new FuturedValidationIdValidator(httpClient, connectorConfig)
  }
}
