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

package uk.gov.hmrc.language

import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.i18n.{DefaultLangs, Lang, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import support.UnitSpec
import uk.gov.hmrc.play.language.LanguageUtils
import uk.gov.hmrc.views.ViewConfig

class ChangeLanguageEndpointSpec extends UnitSpec with ScalaFutures with GuiceOneAppPerSuite {

  "switchTo" should {

    "return Redirect to a Referer with the requested language set " +
      "if the Referer is present in the request" in new Setup {

      assume(viewConfig.languageMap.nonEmpty)

      viewConfig.languageMap foreach { case (languageName, lang) =>
        withClue(s"case when language is: $languageName") {

          val result = await(controller.switchToLanguage(languageName)(request.withHeaders(REFERER -> "referer-url")))

          status(result) shouldBe SEE_OTHER
          result.newCookies.head.name shouldBe "PLAY_LANG"
          result.newCookies.head.value shouldBe lang.code

        }
      }
    }

    "return Redirect to a Referer with the default lang " +
      "if the Referer is present in the request but requested language is unknown" in new Setup {

      val result: Result = await(controller.switchToLanguage("non-defined-lang")(request.withHeaders(REFERER -> "referer-url")))

      status(result) shouldBe SEE_OTHER
      result.newCookies.head.name shouldBe "PLAY_LANG"
      result.newCookies.head.value shouldBe Lang.defaultLang.language

    }

    "produce Internal server error" +
      "if there is no Referer in the request" in new Setup {

      a [RuntimeException] should be thrownBy {
        await(controller.switchToLanguage(viewConfig.languageMap.head._1)(request))
      }

    }
  }

  private trait Setup {

    val viewConfig: ViewConfig = app.injector.instanceOf[ViewConfig]
    implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

    val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

    lazy val testConfig: Map[String, Any] = Map(
      "dwp.originLabel" -> "dwp-iv",
      "play.i18n.langs" -> List("en", "cy"),
      "play.i18n.descriptions" -> Map("en" -> "english", "cy" -> "cymraeg")
    )
    val languageUtils = new LanguageUtils(new DefaultLangs(Seq(Lang("en"), Lang("cy"))), Configuration.from(testConfig))

    val controller = new ChangeLanguageEndpoint(viewConfig, languageUtils, stubControllerComponents())
  }
}
