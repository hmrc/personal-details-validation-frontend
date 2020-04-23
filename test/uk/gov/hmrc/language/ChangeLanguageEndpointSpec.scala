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

package uk.gov.hmrc.language

import akka.stream.Materializer
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Configuration, Environment}
import play.api.i18n.{Lang, Langs, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, Flash, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import setups.views.ViewConfigMockFactory
import uk.gov.hmrc.errorhandling.ErrorHandler
import uk.gov.hmrc.play.language.LanguageUtils.FlashWithSwitchIndicator
import support.UnitSpec
import uk.gov.hmrc.config.{AppConfig, DwpMessagesApi}
import uk.gov.hmrc.views.ViewConfig

class ChangeLanguageEndpointSpec
  extends UnitSpec
    with ScalaFutures
    with GuiceOneAppPerSuite {

  "switchTo" should {

    "return Redirect to a Referer with the requested language set " +
      "if the Referer is present in the request" in new Setup {
      assume(viewConfig.languagesMap.nonEmpty)

      viewConfig.languagesMap foreach { case (languageName, lang) =>
        withClue(s"case when language is: $languageName") {

          val result = await(controller.switchTo(languageName)(request.withHeaders(REFERER -> "referer-url")))

          status(result) shouldBe SEE_OTHER
          result.header.headers("Set-Cookie") should include(s"PLAY_LANG=${lang.code}")
        }
      }
    }

    "return Redirect to a Referer with the default lang " +
      "if the Referer is present in the request but requested language is unknown" in new Setup {
      val redirect = mock[Result]

      val result = await(controller.switchTo("non-defined-lang")(request.withHeaders(REFERER -> "referer-url")))

      status(result) shouldBe SEE_OTHER
      result.header.headers("Set-Cookie") should include(s"PLAY_LANG=${Lang.defaultLang.code}")    }

    "return Bad request with Internal server error page " +
      "if there is no Referer in the request" in new Setup {
      (errorHandler.internalServerErrorTemplate(_: Request[_]))
        .expects(request)
        .returning(Html("error page"))

      val result = await(controller.switchTo(viewConfig.languagesMap.head._1)(request))

      status(result) shouldBe BAD_REQUEST
      bodyOf(result) shouldBe "error page"
    }
  }

  private trait Setup extends MockFactory {
    implicit val materializer: Materializer = mock[Materializer]

    implicit val dwpMessagesApi: DwpMessagesApi = app.injector.instanceOf[DwpMessagesApi]

    val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    val viewConfig: ViewConfig = ViewConfigMockFactory()

    lazy val testConfig: Map[String, Any] = Map("dwp.originLabel" -> "dwp-iv")

    lazy val appConfig = new AppConfig(Configuration.from(testConfig))

    val errorHandler = mock[ErrorHandler]
    val controller = new ChangeLanguageEndpoint(viewConfig, errorHandler, appConfig)
  }
}
