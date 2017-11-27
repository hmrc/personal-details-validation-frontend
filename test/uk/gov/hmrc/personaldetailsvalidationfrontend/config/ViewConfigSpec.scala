/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.personaldetailsvalidationfrontend.config

import play.api.Configuration
import uk.gov.hmrc.play.test.UnitSpec

class ViewConfigSpec extends UnitSpec {

  Seq(
    TestDefinition("analyticsHost", (config: ViewConfig) => config.analyticsHost, "google-analytics.host"),
    TestDefinition("analyticsToken", (config: ViewConfig) => config.analyticsToken, "google-analytics.token"),
    TestDefinition("assetsUrl", (config: ViewConfig) => config.assetsUrl, "assets.url"),
    TestDefinition("assetsVersion", (config: ViewConfig) => config.assetsVersion, "assets.version")
  ) foreach { data =>
    s"${data.propertyName}" should {

      s"return value associated with '${data.configKey}'" in new Setup {
        whenConfigEntriesExists(data.configKey -> "some-value") { config =>
          data.propertyAccessor(config) shouldBe "some-value"
        }
      }

      s"throw a runtime exception when there's no value for '${data.configKey}'" in new Setup {
        whenConfigEntriesExists() { config =>
          a[RuntimeException] should be thrownBy data.propertyAccessor(config)
        }
      }
    }
  }

  "optimizelyBaseUrl" should {

    "return value associated with 'optimizely.url'" in new Setup {
      whenConfigEntriesExists("optimizely.url" -> "some-value") { config =>
        config.optimizelyBaseUrl shouldBe "some-value"
      }
    }

    "return empty String if there's no value for 'optimizely.url'" in new Setup {
      whenConfigEntriesExists() { config =>
        config.optimizelyBaseUrl shouldBe ""
      }
    }
  }

  "optimizelyProjectId" should {

    "return value associated with 'optimizely.projectId'" in new Setup {
      whenConfigEntriesExists("optimizely.projectId" -> "some-value") { config =>
        config.optimizelyProjectId shouldBe Some("some-value")
      }
    }

    "return None if there's no value for 'optimizely.projectId'" in new Setup {
      whenConfigEntriesExists() { config =>
        config.optimizelyProjectId shouldBe None
      }
    }
  }

  "reportAProblemPartialUrl" should {

    "return comprised of 'contact-frontend' host and 'personal-details-validation-frontend'" in new Setup {
      whenConfigEntriesExists(
        "microservice.services.contact-frontend.host" -> "some-host",
        "microservice.services.contact-frontend.port" -> "123") { config =>
        config.reportAProblemPartialUrl shouldBe "http://some-host:123/contact/problem_reports_ajax?service=personal-details-validation-frontend"
      }
    }
    "return comprised of 'contact-frontend' host and 'personal-details-validation-frontend' when 'microservice.services.contact-frontend.protocol' is given" in new Setup {
      whenConfigEntriesExists(
        "microservice.services.contact-frontend.protocol" -> "some-protocol",
        "microservice.services.contact-frontend.host" -> "some-host",
        "microservice.services.contact-frontend.port" -> "123") { config =>
        config.reportAProblemPartialUrl shouldBe "some-protocol://some-host:123/contact/problem_reports_ajax?service=personal-details-validation-frontend"
      }
    }
    "return comprised of 'contact-frontend' host and 'personal-details-validation-frontend' when 'microservice.services.protocol' is given" in new Setup {
      whenConfigEntriesExists(
        "microservice.services.protocol" -> "some-protocol",
        "microservice.services.contact-frontend.host" -> "some-host",
        "microservice.services.contact-frontend.port" -> "123") { config =>
        config.reportAProblemPartialUrl shouldBe "some-protocol://some-host:123/contact/problem_reports_ajax?service=personal-details-validation-frontend"
      }
    }
    "throw a runtime exception when there's no value for 'contact-frontend.host'" in new Setup {
      whenConfigEntriesExists("microservice.services.contact-frontend.port" -> "123") { config =>
        a[RuntimeException] should be thrownBy config.reportAProblemPartialUrl
      }
    }
    "throw a runtime exception when there's no value for 'contact-frontend.port'" in new Setup {
      whenConfigEntriesExists("microservice.services.contact-frontend.host" -> "some-host") { config =>
        a[RuntimeException] should be thrownBy config.reportAProblemPartialUrl
      }
    }
  }

  "reportAProblemNonJSUrl" should {

    "return comprised of 'contact-frontend' host and 'personal-details-validation-frontend'" in new Setup {
      whenConfigEntriesExists(
        "microservice.services.contact-frontend.host" -> "some-host",
        "microservice.services.contact-frontend.port" -> "123") { config =>
        config.reportAProblemNonJSUrl shouldBe "http://some-host:123/contact/problem_reports_nonjs?service=personal-details-validation-frontend"
      }
    }
    "return comprised of 'contact-frontend' host and 'personal-details-validation-frontend' when 'microservice.services.contact-frontend.protocol' is given" in new Setup {
      whenConfigEntriesExists(
        "microservice.services.contact-frontend.protocol" -> "some-protocol",
        "microservice.services.contact-frontend.host" -> "some-host",
        "microservice.services.contact-frontend.port" -> "123") { config =>
        config.reportAProblemNonJSUrl shouldBe "some-protocol://some-host:123/contact/problem_reports_nonjs?service=personal-details-validation-frontend"
      }
    }
    "return comprised of 'contact-frontend' host and 'personal-details-validation-frontend' when 'microservice.services.protocol' is given" in new Setup {
      whenConfigEntriesExists(
        "microservice.services.protocol" -> "some-protocol",
        "microservice.services.contact-frontend.host" -> "some-host",
        "microservice.services.contact-frontend.port" -> "123") { config =>
        config.reportAProblemNonJSUrl shouldBe "some-protocol://some-host:123/contact/problem_reports_nonjs?service=personal-details-validation-frontend"
      }
    }
    "throw a runtime exception when there's no value for 'contact-frontend.host'" in new Setup {
      whenConfigEntriesExists("microservice.services.contact-frontend.port" -> "123") { config =>
        a[RuntimeException] should be thrownBy config.reportAProblemNonJSUrl
      }
    }
    "throw a runtime exception when there's no value for 'contact-frontend.port'" in new Setup {
      whenConfigEntriesExists("microservice.services.contact-frontend.host" -> "some-host") { config =>
        a[RuntimeException] should be thrownBy config.reportAProblemNonJSUrl
      }
    }
  }


  private case class TestDefinition(propertyName: String,
                                    propertyAccessor: ViewConfig => String,
                                    configKey: String)
  private trait Setup {

    def whenConfigEntriesExists(entries: (String, String)*)
                               (testBody: ViewConfig => Unit): Unit =
      testBody(new ViewConfig(Configuration.from(entries.toMap)))
  }
}
