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

package uk.gov.hmrc.errorhandling

import support.UnitSpec
import uk.gov.hmrc.personaldetailsvalidation.model.QueryParamConverter

class ProcessingErrorSpec extends UnitSpec {

  "ProcessingError" should {

    "store the provided message" in {
      val error = ProcessingError("something went wrong")

      error.message shouldBe "something went wrong"
    }

    "convert to technicalError query parameter via implicit QueryParamConverter" in {
      import ProcessingError.queryParamConverter
      import QueryParamConverter.*

      val error = ProcessingError("anything")
      val converter: QueryParamConverter[ProcessingError] = implicitly[QueryParamConverter[ProcessingError]]

      val paramsDirect = converter.toQueryParam(error)
      val paramsOps = error.toQueryParam

      paramsDirect shouldBe Map("technicalError" -> Seq(""))
      paramsOps shouldBe Map("technicalError" -> Seq(""))
    }
  }
}
