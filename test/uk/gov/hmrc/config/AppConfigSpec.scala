/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.config

import play.api.Configuration
import support.UnitSpec

class AppConfigSpec  extends UnitSpec {

  "AppConfig" should {

    "return that the post code lookup is disabled by default." in new Setup {
      appConfig.isPostCodeLookupEnabled shouldBe false
    }

    "return that the post code lookup is enabled when specified." in new Setup {
      override val testConfig = Map("feature.postcode-lookup" -> "true")
      appConfig.isPostCodeLookupEnabled shouldBe true
    }

    "return that the post code lookup is disabled when specified." in new Setup {
      override val testConfig = Map("feature.postcode-lookup" -> "false")
      appConfig.isPostCodeLookupEnabled shouldBe false
    }
  }

  trait Setup {
    val testConfig: Map[String, Any] = Map.empty

    lazy val appConfig = new AppConfig(Configuration.from(testConfig))
  }
}
