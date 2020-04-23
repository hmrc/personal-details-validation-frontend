/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.views

import org.scalamock.scalatest.MockFactory
import org.scalatest.prop.{TableDrivenPropertyChecks, Tables}
import play.api.{Configuration, Environment}
import play.api.i18n.{DefaultLangs, Lang, MessagesApi}
import setups.configs.ConfigSetup
import support.UnitSpec
import uk.gov.hmrc.config.DwpMessagesApi

class ViewConfigSpec
  extends UnitSpec
    with TableDrivenPropertyChecks
    with MockFactory {

  private val scenarios = Tables.Table(
    ("propertyName",   "propertyAccessor",                            "configKey"),
    ("analyticsHost",  (config: ViewConfig) => config.analyticsHost,  "google-analytics.host"),
    ("analyticsToken", (config: ViewConfig) => config.analyticsToken, "google-analytics.token")
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

    "throw a runtime exception when there's no messages file defined for a code from 'play.i18n.langs'" in new Setup2 {
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
    val configuration = Configuration.from(Map("play.i18n.langs" -> List("en", "cy"), "play.i18n.path" -> null))
    val messagesApi = new DwpMessagesApi(Environment.simple(), configuration, new DefaultLangs(configuration))
    val newConfigObject: Configuration => ViewConfig = new ViewConfig(_, messagesApi)

    def expectMessagesFilesExistsFor(codes: String*) = {
      messagesApi.messages
    }
  }

  private trait Setup2 extends ConfigSetup[ViewConfig] {
    val configuration = Configuration.from(Map("play.i18n.langs" -> List("default"), "play.i18n.path" -> null))
    val messagesApi = new DwpMessagesApi(Environment.simple(), configuration, new DefaultLangs(configuration))
    val newConfigObject: Configuration => ViewConfig = new ViewConfig(_, messagesApi)

    def expectMessagesFilesExistsFor(codes: String*) = {
      messagesApi.messages
    }
  }
}
