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

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.Configuration
import play.api.libs.json.{JsObject, Json, Writes}
import support.Generators.Implicits._
import support.UnitSpec
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads}
import uk.gov.hmrc.personaldetailsvalidation.generators.ObjectGenerators._
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators._
import uk.gov.hmrc.personaldetailsvalidation.model._

import scala.concurrent.ExecutionContext.Implicits.{global => executionContext}
import scala.concurrent.{ExecutionContext, Future}

class PersonalDetailsSenderSpec extends UnitSpec with MockFactory with ScalaFutures {

  "submitValidationRequest" should {

    "returned SuccessfulPersonalDetailsValidation from POST to /personal-details-validation with nino" in new Setup {

      val validationId: ValidationId = validationIds.generateOne

      (mockHttpClient.POST[PersonalDetails, PersonalDetailsValidation](_: String, _: PersonalDetails, _: Seq[(String, String)])(_: Writes[PersonalDetails],
        _: HttpReads[PersonalDetailsValidation], _: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *, *, *, *)
        .returning(Future.successful(SuccessfulPersonalDetailsValidation(validationId)))

      connector.submitValidationRequest(personalDetailsWithNino, origin).futureValue shouldBe SuccessfulPersonalDetailsValidation(validationId)
    }

    "returned FailedPersonalDetailsValidation from POST to /personal-details-validation with nino" in new Setup {

      val validationId: ValidationId = validationIds.generateOne

      (mockHttpClient.POST[PersonalDetails, PersonalDetailsValidation](_: String, _: PersonalDetails, _: Seq[(String, String)])(_: Writes[PersonalDetails],
        _: HttpReads[PersonalDetailsValidation], _: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *, *, *, *)
        .returning(Future.successful(FailedPersonalDetailsValidation(validationId)))

      connector.submitValidationRequest(personalDetailsWithNino, origin).futureValue shouldBe FailedPersonalDetailsValidation(validationId)
    }

    "returned SuccessfulPersonalDetailsValidation from POST to /personal-details-validation with postcode" in new Setup {

      val validationId: ValidationId = validationIds.generateOne

      (mockHttpClient.POST[PersonalDetails, PersonalDetailsValidation](_: String, _: PersonalDetails, _: Seq[(String, String)])(_: Writes[PersonalDetails],
        _: HttpReads[PersonalDetailsValidation], _: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *, *, *, *)
        .returning(Future.successful(SuccessfulPersonalDetailsValidation(validationId)))

      connector.submitValidationRequest(personalDetailsWithPostcode, origin).futureValue shouldBe SuccessfulPersonalDetailsValidation(validationId)
    }

    "returned FailedPersonalDetailsValidation from POST to /personal-details-validation with postcode" in new Setup {

      val validationId: ValidationId = validationIds.generateOne

      (mockHttpClient.POST[PersonalDetails, PersonalDetailsValidation](_: String, _: PersonalDetails, _: Seq[(String, String)])(_: Writes[PersonalDetails],
        _: HttpReads[PersonalDetailsValidation], _: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *, *, *, *)
        .returning(Future.successful(FailedPersonalDetailsValidation(validationId)))

      connector.submitValidationRequest(personalDetailsWithPostcode, origin).futureValue shouldBe FailedPersonalDetailsValidation(validationId)
    }
  }

  trait Setup {
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

    val origin = "test"
    val personalDetailsWithNino: PersonalDetailsWithNino = personalDetailsObjects.generateOne

    val payloadWithNino: JsObject = Json.obj(
      "firstName" -> personalDetailsWithNino.firstName.toString(),
      "lastName" -> personalDetailsWithNino.lastName.toString(),
      "dateOfBirth" -> personalDetailsWithNino.dateOfBirth,
      "nino" -> personalDetailsWithNino.nino.toString()
    )

    val personalDetailsWithPostcode: PersonalDetailsWithPostcode = personalDetailsObjectsWithPostcode.generateOne

    val payloadWithPostcode: JsObject = Json.obj(
      "firstName" -> personalDetailsWithPostcode.firstName.toString(),
      "lastName" -> personalDetailsWithPostcode.lastName.toString(),
      "dateOfBirth" -> personalDetailsWithPostcode.dateOfBirth,
      "postCode" -> personalDetailsWithPostcode.postCode.toString()
    )

    private val connectorConfig = new ConnectorConfig(mock[Configuration]) {
      override lazy val personalDetailsValidationBaseUrl = "http://host"
    }

    val mockHttpClient: HttpClient = mock[HttpClient]

    val connector = new PersonalDetailsSender(mockHttpClient, connectorConfig)
  }
}
