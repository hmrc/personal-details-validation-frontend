/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.http

import org.scalamock.scalatest.MockFactory
import play.api.Configuration
import play.api.mvc.EssentialFilter
import support.UnitSpec
import uk.gov.hmrc.play.bootstrap.frontend.filters.FrontendFilters

class PersonalDetailsValidationFiltersSpecs extends UnitSpec with MockFactory {

  "filters" should {
    "include filters from FrontendFilters and AddGaUserIdInHeaderFilter" in new Setup {
      personalDetailsValidationFilters.filters shouldBe originalFilters :+ addGaUserIdInHeaderFilter
    }
  }

  trait Setup {

    val filter1 = mock[EssentialFilter]
    val filter2 = mock[EssentialFilter]
    val filter3 = mock[EssentialFilter]
    val filter4 = mock[EssentialFilter]
    val addGaUserIdInHeaderFilter = mock[AddGaUserIdInHeaderFilter]

    val originalFilters = Seq(filter1, filter2, filter3, filter4)

    val configuration = Configuration.from(Map(
      "security.headers.filter.enabled" -> false,
      "bootstrap.filters.csrf.enabled" -> true,
      "bootstrap.filters.allowlist.enabled" -> false,
      "bootstrap.filters.sessionId.enabled" -> false
    ))

    val frontendFilters = new FrontendFilters(configuration, null, null, null, null, null, null, null, null, null, null, null, null, null){
      override val filters = originalFilters
    }

    val personalDetailsValidationFilters = new PersonalDetailsValidationFilters(frontendFilters, addGaUserIdInHeaderFilter)
  }

}
