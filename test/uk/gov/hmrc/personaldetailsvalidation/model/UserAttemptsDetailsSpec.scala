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

import play.api.libs.json.Json
import support.UnitSpec

class UserAttemptsDetailsSpec extends UnitSpec {

  "UserAttemptsDetails" should {

    "serialise to JSON with a credId" in {
      val details = UserAttemptsDetails(attempts = 2, maybeCredId = Some("cred-abc"))
      val json    = Json.toJson(details)

      (json \ "attempts").as[Int]            shouldBe 2
      (json \ "maybeCredId").as[String]      shouldBe "cred-abc"
    }

    "serialise to JSON without a credId" in {
      val details = UserAttemptsDetails(attempts = 1, maybeCredId = None)
      val json    = Json.toJson(details)

      (json \ "attempts").as[Int]         shouldBe 1
      (json \ "maybeCredId").toOption     shouldBe None
    }

    "deserialise from JSON with a credId" in {
      val json = Json.parse("""{"attempts":3,"maybeCredId":"cred-xyz"}""")
      json.as[UserAttemptsDetails] shouldBe UserAttemptsDetails(3, Some("cred-xyz"))
    }

    "deserialise from JSON without a credId" in {
      val json = Json.parse("""{"attempts":0}""")
      json.as[UserAttemptsDetails] shouldBe UserAttemptsDetails(0, None)
    }

    "round-trip through JSON" in {
      val original = UserAttemptsDetails(5, Some("cred-round"))
      Json.toJson(original).as[UserAttemptsDetails] shouldBe original
    }
  }
}
