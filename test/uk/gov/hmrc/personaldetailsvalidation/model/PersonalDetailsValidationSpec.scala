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

package uk.gov.hmrc.personaldetailsvalidation.model

import play.api.libs.json.{JsResultException, Json}
import support.UnitSpec

class PersonalDetailsValidationSpec extends UnitSpec {

  "PersonalDetailsValidation reads" should {

    "parse a successful validation response" in {
      val json = Json.parse(
        """
          |{
          |  "validationStatus": "success",
          |  "id": "test-id-1",
          |  "credentialId": "cred-1",
          |  "attempts": 1,
          |  "deceased": false
          |}
        """.stripMargin
      )

      val result = json.as[PersonalDetailsValidation]

      result shouldBe SuccessfulPersonalDetailsValidation(ValidationId("test-id-1"), deceased = false)
    }

    "parse a successful validation response with deceased set to true" in {
      val json = Json.parse(
        """
          |{
          |  "validationStatus": "success",
          |  "id": "test-id-2",
          |  "credentialId": "cred-2",
          |  "attempts": 0,
          |  "deceased": true
          |}
        """.stripMargin
      )

      val result = json.as[PersonalDetailsValidation]

      result shouldBe SuccessfulPersonalDetailsValidation(ValidationId("test-id-2"), deceased = true)
    }

    "default deceased to false when the field is absent from a success response" in {
      val json = Json.parse(
        """
          |{
          |  "validationStatus": "success",
          |  "id": "test-id-3"
          |}
        """.stripMargin
      )

      val result = json.as[PersonalDetailsValidation]

      result shouldBe SuccessfulPersonalDetailsValidation(ValidationId("test-id-3"), deceased = false)
    }

    "parse a failed validation response" in {
      val json = Json.parse(
        """
          |{
          |  "validationStatus": "failure",
          |  "id": "test-id-4",
          |  "credentialId": "cred-4",
          |  "attempts": 3,
          |  "deceased": false
          |}
        """.stripMargin
      )

      val result = json.as[PersonalDetailsValidation]

      result shouldBe FailedPersonalDetailsValidation(ValidationId("test-id-4"), "cred-4", 3)
    }

    "default credentialId to empty string and attempts to 0 when absent in a failure response" in {
      val json = Json.parse(
        """
          |{
          |  "validationStatus": "failure",
          |  "id": "test-id-5"
          |}
        """.stripMargin
      )

      val result = json.as[PersonalDetailsValidation]

      result shouldBe FailedPersonalDetailsValidation(ValidationId("test-id-5"), "", 0)
    }

    "throw a RuntimeException for an unrecognised validationStatus" in {
      val json = Json.parse(
        """
          |{
          |  "validationStatus": "unknown",
          |  "id": "test-id-6"
          |}
        """.stripMargin
      )

      a[RuntimeException] should be thrownBy json.as[PersonalDetailsValidation]
    }
  }
}
