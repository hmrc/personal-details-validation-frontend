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

package uk.gov.hmrc.config

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import support.UnitSpec

class AppConfigSpec extends UnitSpec with GuiceOneAppPerSuite {

  val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  "AppConfig" should {
    "return logout page." in {
      appConfig.logoutPage shouldBe "https://www.ete.access.service.gov.uk/logout"
    }

    "return basGatewayUrl." in {
      appConfig.basGatewayUrl shouldBe "http://localhost:9553"
    }

    "return logoutPath." in new {
      appConfig.logoutPath shouldBe "/bas-gateway/sign-out-without-state"
    }
  }
}