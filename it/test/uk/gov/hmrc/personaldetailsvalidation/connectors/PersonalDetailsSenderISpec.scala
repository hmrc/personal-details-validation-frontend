/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.json.Json
import play.api.test.Helpers.*
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators.*
import uk.gov.hmrc.personaldetailsvalidation.model.*
import uk.gov.hmrc.personaldetailsvalidation.utils.ComponentSpecHelper
import uk.gov.hmrc.personaldetailsvalidation.utils.PersonalDetailsHelper.personalDetailsWrites

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.ExecutionContext

class PersonalDetailsSenderISpec extends ComponentSpecHelper {

  "submitValidationRequest" should {

    "manage a successful personal details validation from POST to /personal-details-validation with nino" in new Setup {

      stubPost(submitValidationRequestUrl)(OK, Some(successfulPersonalDetailsValidation))

      val expected: SuccessfulPersonalDetailsValidation = SuccessfulPersonalDetailsValidation(ValidationId(testValidationId))

      val actual: PersonalDetailsValidation = await(personalDetailsSender.submitValidationRequest(testPersonalDetailsWithNino, testOrigin, hc))

      actual shouldBe expected

      val expectedRequestBody: String = Json.toJson(testPersonalDetailsWithNino)(using personalDetailsWrites).toString()

      verifyPost(submitValidationRequestUrl, Some(expectedRequestBody))
    }

    "manage a failed personal details validation from POST to /personal-details-validation with post code" in new Setup {

      stubPost(submitValidationRequestUrl)(OK, Some(failedPersonalDetailsValidation))

      val expected: FailedPersonalDetailsValidation = FailedPersonalDetailsValidation(ValidationId(testValidationId), testCredId, testAttempts)

      val actual: PersonalDetailsValidation = await(personalDetailsSender.submitValidationRequest(testPersonalDetailsWithNino, testOrigin, hc))

      actual shouldBe expected

      val expectedRequestBody: String = Json.toJson(testPersonalDetailsWithNino)(using personalDetailsWrites).toString()

      verifyPost(submitValidationRequestUrl, Some(expectedRequestBody))
    }

    "raise an error" when {

      "a response of bad request from POST to /personal-details-validation with nino is returned" in new Setup {

        stubPost(submitValidationRequestUrl)(BAD_REQUEST)

        val error: UpstreamErrorResponse = intercept[UpstreamErrorResponse](await(personalDetailsSender.submitValidationRequest(testPersonalDetailsWithNino, testOrigin, hc)))

        error match {
          case UpstreamErrorResponse(_, BAD_REQUEST, _, _) => succeed
          case other => fail(s"Unexpected error returned : $other")
        }

      }

      "a response of internal server error from POST to /personal-details-validation with nino is returned" in new Setup {

        stubPost(submitValidationRequestUrl)(INTERNAL_SERVER_ERROR)

        val error: UpstreamErrorResponse = intercept[UpstreamErrorResponse](await(personalDetailsSender.submitValidationRequest(testPersonalDetailsWithNino, testOrigin, hc)))

        error match {
          case UpstreamErrorResponse(_, INTERNAL_SERVER_ERROR, _, _) => succeed
          case other => fail(s"Unexpected error returned : $other")
        }

      }
    }

    "manage a successful personal details validation from POST to /personal-details-validation with post code" in new Setup {

      stubPost(submitValidationRequestUrl)(OK, Some(successfulPersonalDetailsValidation))

      val expected: SuccessfulPersonalDetailsValidation = SuccessfulPersonalDetailsValidation(ValidationId(testValidationId))

      val actual: PersonalDetailsValidation = await(personalDetailsSender.submitValidationRequest(testPersonalDetailsWithPostCode, testOrigin, hc))

      actual shouldBe expected

      val expectedRequestBody: String = Json.toJson(testPersonalDetailsWithPostCode)(using personalDetailsWrites).toString()

      verifyPost(submitValidationRequestUrl, Some(expectedRequestBody))
    }

    "manage a failed personal details validation from POST to /personal-details-validation with nino" in new Setup {

      stubPost(submitValidationRequestUrl)(OK, Some(failedPersonalDetailsValidation))

      val expected: FailedPersonalDetailsValidation = FailedPersonalDetailsValidation(ValidationId(testValidationId), testCredId, testAttempts)

      val actual: PersonalDetailsValidation = await(personalDetailsSender.submitValidationRequest(testPersonalDetailsWithPostCode, testOrigin, hc))

      actual shouldBe expected

      val expectedRequestBody: String = Json.toJson(testPersonalDetailsWithPostCode)(using personalDetailsWrites).toString()

      verifyPost(submitValidationRequestUrl, Some(expectedRequestBody))
    }
  }

  "getUserAttempts" should {

    "successfully return an instance of UserAttemptDetails with a credential identifier" in new Setup {

      stubGet(getUserAttemptsUrl)(OK, Some(userAttemptsWithCredId))

      val actual: UserAttemptsDetails = await(personalDetailsSender.getUserAttempts())

      val expected: UserAttemptsDetails = UserAttemptsDetails(testAttempts, Some(testCredId))

      actual shouldBe expected

      verifyGet(getUserAttemptsUrl)
    }

    "successfully return an instance of UserAttemptDetails without a credential identifier" in new Setup {

      stubGet(getUserAttemptsUrl)(OK, Some(userAttemptsWithoutCredId))

      val actual: UserAttemptsDetails = await(personalDetailsSender.getUserAttempts())

      val expected: UserAttemptsDetails = UserAttemptsDetails(testAttempts, None)

      actual shouldBe expected

      verifyGet(getUserAttemptsUrl)
    }

    "raise an error" when {

      "a response of bad request is returned" in new Setup {

        stubGet(getUserAttemptsUrl)(BAD_REQUEST)

        val error: UpstreamErrorResponse = intercept[UpstreamErrorResponse](await(personalDetailsSender.getUserAttempts()))

        error match {
          case UpstreamErrorResponse(_, BAD_REQUEST, _, _) => succeed
          case other => fail(s"Unexpected error returned : $other")
        }

      }

      "a response of internet server error is returned" in new Setup {

        stubGet(getUserAttemptsUrl)(INTERNAL_SERVER_ERROR)

        val error: UpstreamErrorResponse = intercept[UpstreamErrorResponse](await(personalDetailsSender.getUserAttempts()))

        error match {
          case UpstreamErrorResponse(_, INTERNAL_SERVER_ERROR, _, _) => succeed
          case other => fail(s"Unexpected error returned : $other")
        }

      }

    }

  }


  trait Setup {

    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val ec: ExecutionContext = ExecutionContext.global

    val submitValidationRequestUrl: String = "/personal-details-validation"
    val getUserAttemptsUrl: String = "/personal-details-validation/get-user-attempts"

    val testFirstName: String = "Test First Name"
    val testLastName: String = "Test Last Name"
    val testDataOfBirth: String = "2000-01-01"
    val testNino: Option[Nino] = ninos.sample
    val testPostCode: String = "AA1 1AA"

    val testPersonalDetailsWithNino: PersonalDetails = PersonalDetailsWithNino(
      NonEmptyString(testFirstName),
      NonEmptyString(testLastName),
      testNino.getOrElse(Nino("")),
      LocalDate.parse(testDataOfBirth)
    )

    val testPersonalDetailsWithPostCode: PersonalDetails = PersonalDetailsWithPostcode(
      NonEmptyString(testFirstName),
      NonEmptyString(testLastName),
      NonEmptyString(testPostCode),
      LocalDate.parse(testDataOfBirth)
    )

    val testOrigin: String = "ma"

    val expectedPersonalDetailsWithNinoAsJson: String =
      s"""
         |{
         |  "firstName" : "$testFirstName",
         |  "lastName" : "$testLastName",
         |  "dateOfBirth" : "$testDataOfBirth",
         |  "nino" : "$testNino"
         |}
         |""".stripMargin

    val testSuccess: String = "success"
    val testFailure: String = "failure"
    val testValidationId: String = UUID.randomUUID().toString
    val testCredId: String = "credId-1"
    val testAttempts: Int = 1

    val successfulPersonalDetailsValidation: String =
      s"""
         |{
         |  "validationStatus" : "$testSuccess",
         |  "id" : "$testValidationId",
         |  "credentialId" : "$testCredId",
         |  "attempts" : $testAttempts,
         |  "deceased" : false
         |}
         |""".stripMargin

    val failedPersonalDetailsValidation: String =
      s"""
         |{
         |  "validationStatus" : "$testFailure",
         |  "id" : "$testValidationId",
         |  "credentialId" : "$testCredId",
         |  "attempts" : $testAttempts,
         |  "deceased" : false
         |}
         |""".stripMargin

    val userAttemptsWithCredId: String =
      s"""
         |{
         |  "attempts" : $testAttempts,
         |  "maybeCredId" : "$testCredId"
         |}
         |""".stripMargin

    val userAttemptsWithoutCredId: String =
      s"""
         |{
         |  "attempts" : $testAttempts
         |}
         |""".stripMargin

    val personalDetailsSender: PersonalDetailsSender = app.injector.instanceOf[PersonalDetailsSender]
  }

}
