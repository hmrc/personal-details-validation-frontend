/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.personaldetailsvalidation.endpoints

import play.api.Configuration
import play.api.test.FakeRequest
import support.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.personaldetailsvalidation.connectors.{ConnectorConfig, PersonalDetailsSender}
import uk.gov.hmrc.personaldetailsvalidation.model.*

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PersonalDetailsSubmissionSpec extends UnitSpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  // A ConnectorConfig stub so PersonalDetailsSender can initialise without real config
  private val stubConnectorConfig = new ConnectorConfig(Configuration.empty) {
    override lazy val personalDetailsValidationBaseUrl: String = "http://stub"
  }

  "successResult" should {

    // Only successResult is tested here without a connector — the connector is never touched
    val submission = new PersonalDetailsSubmission(null)

    "redirect to the completionUrl with validationId appended and add validationId to session" in {
      implicit val request = FakeRequest()

      val validationId  = ValidationId(UUID.randomUUID().toString)
      val completionUrl = CompletionUrl.completionUrl("/some/path").fold(throw _, identity)
      val pdv           = SuccessfulPersonalDetailsValidation(validationId)

      val result = submission.successResult(completionUrl, pdv)

      result.header.status shouldBe 303
      result.header.headers.get("Location") should contain(s"/some/path?validationId=${validationId.value}")
    }

    "strip an existing validationId from the completionUrl before redirecting" in {
      val existingId = UUID.randomUUID().toString
      val newId      = ValidationId(UUID.randomUUID().toString)

      implicit val request = FakeRequest()

      val completionUrl = CompletionUrl.completionUrl(s"/some/path?validationId=$existingId").fold(throw _, identity)
      val pdv           = SuccessfulPersonalDetailsValidation(newId)

      val result = submission.successResult(completionUrl, pdv)

      val location = result.header.headers.getOrElse("Location", "")
      location should not include existingId
      location should include(newId.value)
    }

    "throw a RuntimeException for a non-successful validation" in {
      implicit val request = FakeRequest()

      val completionUrl = CompletionUrl.completionUrl("/some/path").fold(throw _, identity)
      val pdv           = FailedPersonalDetailsValidation(ValidationId("fail-id"), "cred", 1)

      a[RuntimeException] should be thrownBy submission.successResult(completionUrl, pdv)
    }
  }

  "submitPersonalDetails" should {

    "forward the request with origin from session to the connector" in {
      val details      = PersonalDetailsWithNino(NonEmptyString("John"), NonEmptyString("Smith"), uk.gov.hmrc.domain.Nino("AA000003D"), java.time.LocalDate.of(1980, 1, 1))
      val expectedPdv  = SuccessfulPersonalDetailsValidation(ValidationId("id-1"))

      val stubConnector = new PersonalDetailsSender(null, stubConnectorConfig) {
        override def submitValidationRequest(pd: PersonalDetails, origin: String, hc: HeaderCarrier)(using ec: scala.concurrent.ExecutionContext): Future[PersonalDetailsValidation] = {
          origin shouldBe "ma"
          Future.successful(expectedPdv)
        }
      }

      val submission = new PersonalDetailsSubmission(stubConnector)
      implicit val request = FakeRequest().withSession(("origin", "ma"))

      val result = await(submission.submitPersonalDetails(details))
      result shouldBe expectedPdv
    }

    "use Unknown-Origin when the session has no origin key" in {
      val details     = PersonalDetailsWithPostcode(NonEmptyString("Jane"), NonEmptyString("Doe"), NonEmptyString("SW1A 1AA"), java.time.LocalDate.of(1990, 5, 20))
      val expectedPdv = SuccessfulPersonalDetailsValidation(ValidationId("id-2"))

      val stubConnector = new PersonalDetailsSender(null, stubConnectorConfig) {
        override def submitValidationRequest(pd: PersonalDetails, origin: String, hc: HeaderCarrier)(using ec: scala.concurrent.ExecutionContext): Future[PersonalDetailsValidation] = {
          origin shouldBe "Unknown-Origin"
          Future.successful(expectedPdv)
        }
      }

      val submission = new PersonalDetailsSubmission(stubConnector)
      implicit val request = FakeRequest()

      val result = await(submission.submitPersonalDetails(details))
      result shouldBe expectedPdv
    }
  }

  "getUserAttempts" should {

    "delegate to the connector and return the result" in {
      val expected = UserAttemptsDetails(2, Some("cred-abc"))

      val stubConnector = new PersonalDetailsSender(null, stubConnectorConfig) {
        override def getUserAttempts()(using hc: HeaderCarrier, ec: scala.concurrent.ExecutionContext): Future[UserAttemptsDetails] =
          Future.successful(expected)
      }

      val submission = new PersonalDetailsSubmission(stubConnector)

      val result = await(submission.getUserAttempts())
      result shouldBe expected
    }
  }
}
