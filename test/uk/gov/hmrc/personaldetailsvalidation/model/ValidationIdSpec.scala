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

import support.UnitSpec

class ValidationIdSpec extends UnitSpec {

  "ValidationId" should {

    "return its value from toString" in {
      val id = ValidationId("abc-123")
      id.toString shouldBe "abc-123"
    }

    "produce a validationId query parameter via implicit QueryParamConverter" in {
      import QueryParamConverter.*

      val id = ValidationId("some-id")

      id.toQueryParam shouldBe Map("validationId" -> Seq("some-id"))
    }

    "produce the same result when called directly on the converter" in {
      val id = ValidationId("another-id")
      val converter = implicitly[QueryParamConverter[ValidationId]]

      converter.toQueryParam(id) shouldBe Map("validationId" -> Seq("another-id"))
    }
  }
}
