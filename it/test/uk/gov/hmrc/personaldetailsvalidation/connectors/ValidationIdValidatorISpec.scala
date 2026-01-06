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

import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.personaldetailsvalidation.model.ValidationId
import uk.gov.hmrc.personaldetailsvalidation.utils.ComponentSpecHelper

import java.util.UUID
import scala.concurrent.ExecutionContext

class ValidationIdValidatorISpec extends ComponentSpecHelper {

  "checkExists" should {

    "return true if call to GET /personal-details-validation/:validationId returns OK" in new Setup {

      stubGet(checkExistsUrl)(OK, Some(validationId))

      await(validationIdValidator.checkExists(ValidationId(testValidationId))) shouldBe true

    }

    "return false if call to GET /personal-details-validation/:validationId returns NOT_FOUND" in new Setup {

      stubGet(checkExistsUrl)(NOT_FOUND)

      await(validationIdValidator.checkExists(ValidationId(testValidationId))) shouldBe false

    }

    "raise an error" when {

      "a response status other than ok or not found is returned" in new Setup {

        stubGet(checkExistsUrl)(UNAUTHORIZED)

        val exception = intercept[RuntimeException](await(validationIdValidator.checkExists(ValidationId(testValidationId))))

        exception shouldBe a[RuntimeException]

        exception.getMessage shouldBe s"Unexpected status returned from PDV: $UNAUTHORIZED"
      }
    }

  }

  trait Setup {

    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val ec: ExecutionContext = ExecutionContext.global

    val testValidationId: String = UUID.randomUUID().toString
    val checkExistsUrl: String = s"/personal-details-validation/$testValidationId"

    val validationId: String = s"""{ "value" : "$testValidationId" }"""

    val validationIdValidator: ValidationIdValidator = app.injector.instanceOf[ValidationIdValidator]
  }



}
