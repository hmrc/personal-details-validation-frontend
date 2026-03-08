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

class JourneyUpdateSpec extends UnitSpec {

  "JourneyUpdate" should {

    "serialise to JSON with a journeyStatus" in {
      val update = JourneyUpdate(Some("Timeout"))
      val json   = Json.toJson(update)

      (json \ "journeyStatus").as[String] shouldBe "Timeout"
    }

    "serialise to JSON with None journeyStatus" in {
      val update = JourneyUpdate(None)
      val json   = Json.toJson(update)

      (json \ "journeyStatus").toOption shouldBe None
    }

    "deserialise from JSON with a journeyStatus" in {
      val json = Json.parse("""{"journeyStatus":"Success"}""")
      json.as[JourneyUpdate] shouldBe JourneyUpdate(Some("Success"))
    }

    "deserialise from JSON without a journeyStatus" in {
      val json = Json.parse("""{}""")
      json.as[JourneyUpdate] shouldBe JourneyUpdate(None)
    }

    "round-trip through JSON" in {
      val original = JourneyUpdate(Some("UserAborted"))
      Json.toJson(original).as[JourneyUpdate] shouldBe original
    }
  }
}
