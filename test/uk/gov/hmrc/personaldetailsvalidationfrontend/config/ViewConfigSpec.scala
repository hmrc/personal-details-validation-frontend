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

import org.scalamock.scalatest.MockFactory
import org.scalatest.prop.{TableDrivenPropertyChecks, Tables}
import play.api.Configuration
import play.api.i18n.{Lang, MessagesApi}
import uk.gov.hmrc.personaldetailsvalidationfrontend.test.configs.ConfigSetup
import uk.gov.hmrc.play.test.UnitSpec

class ViewConfigSpec
  extends UnitSpec
    with TableDrivenPropertyChecks
    with MockFactory {

  private val scenarios = Tables.Table(
    ("propertyName",   "propertyAccessor",                            "configKey"),
    ("analyticsHost",  (config: ViewConfig) => config.analyticsHost,  "google-analytics.host"),
    ("analyticsToken", (config: ViewConfig) => config.analyticsToken, "google-analytics.token"),
    ("assetsUrl",      (config: ViewConfig) => config.assetsUrl,      "assets.url"),
    ("assetsVersion",  (config: ViewConfig) => config.assetsVersion,  "assets.version")
  )

  forAll(scenarios) { (propertyName, propertyAccessor, configKey) =>
    s"$propertyName" should {

      s"return value associated with '$configKey'" in new Setup {
        whenConfigEntriesExists(configKey -> "some-value") { config =>
          propertyAccessor(config) shouldBe "some-value"
        }
      }

      s"throw a runtime exception when there's no value for '$configKey'" in new Setup {
        whenConfigEntriesExists() { config =>
          a[RuntimeException] should be thrownBy propertyAccessor(config)
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

  "languagesMap" should {

    "return a map of language descriptions associated with Lang objects" in new Setup {
      expectMessagesFilesExistsFor("default", "cy")

      whenConfigEntriesExists(
        "play.i18n.langs" -> List("en", "cy"),
        "play.i18n.descriptions" -> Map("en" -> "english", "cy" -> "cymraeg")
      ) { config =>
        config.languagesMap shouldBe Map(
          "english" -> Lang("en"),
          "cymraeg" -> Lang("cy")
        )
      }
    }

    "return an empty map when there's no value for 'play.i18n.langs'" in new Setup {
      whenConfigEntriesExists() { config =>
        config.languagesMap shouldBe Map.empty
      }
    }

    "throw a runtime exception when there's no messages file defined for a code from 'play.i18n.langs'" in new Setup {
      expectMessagesFilesExistsFor("default")

      whenConfigEntriesExists(
        "play.i18n.langs" -> List("en", "cy"),
        "play.i18n.descriptions" -> Map("en" -> "english", "cy" -> "cymraeg")
      ) { config =>
        a[RuntimeException] should be thrownBy config.languagesMap
      }
    }

    "throw a runtime exception when there's no language description defined for a code in 'play.i18n.langs'" in new Setup {
      expectMessagesFilesExistsFor("default", "cy")

      whenConfigEntriesExists(
        "play.i18n.langs" -> List("en", "cy", "pl"),
        "play.i18n.descriptions" -> Map("en" -> "english", "cy" -> "cymraeg")
      ) { config =>
        a[RuntimeException] should be thrownBy config.languagesMap
      }
    }
  }

  private trait Setup extends ConfigSetup[ViewConfig] {
    val messagesApi = mock[MessagesApi]
    val newConfigObject: Configuration => ViewConfig = new ViewConfig(_, messagesApi)

    def expectMessagesFilesExistsFor(codes: String*) = {
      val messagesMap = codes.map(_ -> Map.empty[String, String]).toMap
      (messagesApi.messages _)
        .expects()
        .returning(messagesMap)
        .repeat(messagesMap.size)
    }
  }
}
