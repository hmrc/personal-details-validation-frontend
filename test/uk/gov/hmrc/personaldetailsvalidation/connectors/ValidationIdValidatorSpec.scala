/*
 * Copyright 2023 HM Revenue & Customs
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

import support.Generators.Implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.Configuration
import support.UnitSpec
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads}
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators.validationIds
import uk.gov.hmrc.personaldetailsvalidation.model.ValidationId

import scala.concurrent.ExecutionContext.Implicits.{global => executionContext}
import scala.concurrent.{ExecutionContext, Future}

class ValidationIdValidatorSpec extends UnitSpec with MockFactory with ScalaFutures {

  "verify" should {

    "return true if call to GET /personal-details-validation/:validationId returns OK" in new Setup {

      (mockHttpClient.GET[Boolean](_: String, _: Seq[(String, String)], _: Seq[(String, String)])(_: HttpReads[Boolean],
        _: HeaderCarrier, _: ExecutionContext))
        .expects(s"$baseUrl/personal-details-validation/$validationId", *, *, *, *, *)
        .returning(Future.successful(true))

      await(validationIdValidator.checkExists(validationId)) shouldBe true
    }

    "return false if call to GET /personal-details-validation/:validationId returns NOT_FOUND" in new Setup {

      (mockHttpClient.GET[Boolean](_: String, _: Seq[(String, String)], _: Seq[(String, String)])(_: HttpReads[Boolean],
        _: HeaderCarrier, _: ExecutionContext))
        .expects(s"$baseUrl/personal-details-validation/$validationId", *, *, *, *, *)
        .returning(Future.successful(false))

      await(validationIdValidator.checkExists(validationId)) shouldBe false
    }
  }

  private trait Setup {

    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

    val validationId: ValidationId = validationIds.generateOne

    val mockHttpClient: HttpClient = mock[HttpClient]

    val baseUrl = "http://host"
    private val connectorConfig = new ConnectorConfig(mock[Configuration]) {
      override lazy val personalDetailsValidationBaseUrl: String = baseUrl
    }

    val validationIdValidator = new ValidationIdValidator(mockHttpClient, connectorConfig)
  }
}
