/*
 * Copyright 2021 HM Revenue & Customs
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

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import support.UnitSpec

class PlayHttpFilterConfigurationSpecs extends UnitSpec with GuiceOneAppPerSuite {

  "play application configuration" should {
    "point 'play.http.filters' to PersonalDetailsValidationFilters" in {

      val currentConfiguration = fakeApplication.injector.instanceOf[Configuration]

      currentConfiguration.getOptional[String]("play.http.filters") shouldBe Some("uk.gov.hmrc.http.PersonalDetailsValidationFilters")

    }
  }

}
