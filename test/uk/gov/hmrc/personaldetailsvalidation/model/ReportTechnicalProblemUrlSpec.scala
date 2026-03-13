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

import play.api.mvc.Call
import support.UnitSpec

class ReportTechnicalProblemUrlSpec extends UnitSpec {

  private val baseUrl = "https://www.tax.service.gov.uk/contact/report-technical-problem?service=government-gateway-identity-verification-frontend"

  "ReportTechnicalProblemUrl.apply" should {

    "return the base URL with an encoded referrerUrl for a DeskPro-eligible origin" in {
      val call   = Call("GET", "/personal-details-validation/start")
      val result = ReportTechnicalProblemUrl("ma", call)

      result should startWith(baseUrl)
      result should include("referrerUrl=")
    }

    "return just the base URL for a DWP origin (not DeskPro)" in {
      val call   = Call("GET", "/personal-details-validation/start")
      val result = ReportTechnicalProblemUrl("dwp-iv", call)

      result shouldBe baseUrl
    }

    "return just the base URL for a bta-sa origin (not DeskPro)" in {
      val call   = Call("GET", "/personal-details-validation/start")
      val result = ReportTechnicalProblemUrl("bta-sa", call)

      result shouldBe baseUrl
    }

    "return just the base URL for a pta-sa origin (not DeskPro)" in {
      val call   = Call("GET", "/personal-details-validation/start")
      val result = ReportTechnicalProblemUrl("pta-sa", call)

      result shouldBe baseUrl
    }

    "return just the base URL for a ssttp-sa origin (not DeskPro)" in {
      val call   = Call("GET", "/personal-details-validation/start")
      val result = ReportTechnicalProblemUrl("ssttp-sa", call)

      result shouldBe baseUrl
    }

    "URL-encode special characters in the referrerUrl for a DeskPro origin" in {
      val call   = Call("GET", "/some/path?foo=bar&baz=qux")
      val result = ReportTechnicalProblemUrl("pta", call)

      result should include("referrerUrl=")
      result should not include "&baz=qux" // raw ampersand should be encoded
    }
  }
}
