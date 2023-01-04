/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.personaldetailsvalidation.connectors

import org.scalamock.scalatest.MockFactory
import play.api.Configuration
import support.{ConfigSetup, UnitSpec}

class ConnectorConfigSpec
  extends UnitSpec
    with MockFactory {

  "personalDetailsValidationBaseUrl" should {

    "return be comprised of configured host and port" in new Setup {
      whenConfigEntriesExists(
        "microservice.services.protocol" -> "http",
        "microservice.services.personal-details-validation.protocol" -> "http",
        "microservice.services.personal-details-validation.host" -> "some-host",
        "microservice.services.personal-details-validation.port" -> "123") { config =>
        config.personalDetailsValidationBaseUrl shouldBe "http://some-host:123"
      }
    }
    "return be comprised of configured protocol, host and port" in new Setup {
      whenConfigEntriesExists(
        "microservice.services.protocol" -> "http",
        "microservice.services.personal-details-validation.protocol" -> "some-protocol",
        "microservice.services.personal-details-validation.host" -> "some-host",
        "microservice.services.personal-details-validation.port" -> "123") { config =>
        config.personalDetailsValidationBaseUrl shouldBe "some-protocol://some-host:123"
      }
    }
    "return be comprised of configured protocol, host and port when 'microservice.services.protocol' is given" in new Setup {
      whenConfigEntriesExists(
        "microservice.services.protocol" -> "some-protocol",
        "microservice.services.personal-details-validation.protocol" -> "some-protocol",
        "microservice.services.personal-details-validation.host" -> "some-host",
        "microservice.services.personal-details-validation.port" -> "123") { config =>
        config.personalDetailsValidationBaseUrl shouldBe "some-protocol://some-host:123"
      }
    }
    "throw a runtime exception when there's no value for 'personal-details-validation.host'" in new Setup {

      whenConfigEntriesExists(
        "microservice.services.personal-details-validation.port" -> "123",
        "microservice.services.protocol" -> "http") { config =>
        a[RuntimeException] should be thrownBy config.personalDetailsValidationBaseUrl
      }
    }
    "throw a runtime exception when there's no value for 'personal-details-validation.port'" in new Setup {
      whenConfigEntriesExists("microservice.services.personal-details-validation.host" -> "some-host") { config =>
        a[RuntimeException] should be thrownBy config.personalDetailsValidationBaseUrl
      }
    }
  }

  private trait Setup extends ConfigSetup[ConnectorConfig] {
    val newConfigObject: Configuration => ConnectorConfig = new ConnectorConfig(_)
  }
}
