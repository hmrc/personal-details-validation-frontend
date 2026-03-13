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

package uk.gov.hmrc.personaldetailsvalidation.monitoring

import support.UnitSpec

class MonitoringEventsSpec extends UnitSpec {

  "PdvFailedAttempt" should {

    "store the provided properties" in {
      val event = PdvFailedAttempt(
        attempts       = 2,
        maxAttempts    = 3,
        journeyVersion = "NINO",
        credID         = "cred-123",
        origin         = "ma"
      )

      event.attempts        shouldBe 2
      event.maxAttempts     shouldBe 3
      event.journeyVersion  shouldBe "NINO"
      event.credID          shouldBe "cred-123"
      event.origin          shouldBe "ma"
    }

    "be a MonitoringEvent" in {
      val event: MonitoringEvent = PdvFailedAttempt(1, 3, "Postcode", "cred-1", "pta")
      event shouldBe a[MonitoringEvent]
    }
  }

  "PdvLockedOut" should {

    "store the provided properties" in {
      val event = PdvLockedOut(
        journeyVersion = "Postcode",
        credID         = "cred-456",
        origin         = "dwp-iv"
      )

      event.journeyVersion shouldBe "Postcode"
      event.credID         shouldBe "cred-456"
      event.origin         shouldBe "dwp-iv"
    }

    "be a MonitoringEvent" in {
      val event: MonitoringEvent = PdvLockedOut("NINO", "cred-2", "bta-sa")
      event shouldBe a[MonitoringEvent]
    }
  }
}
