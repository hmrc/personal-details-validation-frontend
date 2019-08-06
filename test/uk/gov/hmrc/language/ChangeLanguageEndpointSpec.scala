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

package uk.gov.hmrc.language

import akka.stream.Materializer
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, Flash, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import setups.views.ViewConfigMockFactory
import uk.gov.hmrc.errorhandling.ErrorHandler
import uk.gov.hmrc.play.language.LanguageUtils.FlashWithSwitchIndicator
import support.UnitSpec
import uk.gov.hmrc.views.ViewConfig

class ChangeLanguageEndpointSpec extends UnitSpec with ScalaFutures {

  "switchTo" should {

    "return Redirect to a Referer with the requested language set " +
      "if the Referer is present in the request" in new Setup with Mocks {
      assume(viewConfig.languagesMap.nonEmpty)

      viewConfig.languagesMap foreach { case (languageName, lang) =>
        withClue(s"case when language is: $languageName") {
          val redirect = mock[Result]
          expectRedirect(to = "referer-url", withLang = lang)
            .returning(redirect)

          val result = controller.switchTo(languageName)(request.withHeaders(REFERER -> "referer-url"))

          result.futureValue shouldBe redirect
        }
      }
    }

    "return Redirect to a Referer with the default lang " +
      "if the Referer is present in the request but requested language is unknown" in new Setup with Mocks {
      val redirect = mock[Result]
      expectRedirect(to = "referer-url", withLang = Lang.defaultLang)
        .returning(redirect)

      val result = controller.switchTo("non-defined-lang")(request.withHeaders(REFERER -> "referer-url"))

      result.futureValue shouldBe redirect
    }

    "return Bad request with Internal server error page " +
      "if there is no Referer in the request" in new Setup {
      (errorHandler.internalServerErrorTemplate(_: Request[_]))
        .expects(request)
        .returning(Html("error page"))

      val result = controller.switchTo(viewConfig.languagesMap.head._1)(request)

      status(result) shouldBe BAD_REQUEST
      bodyOf(result).futureValue shouldBe "error page"
    }
  }

  private trait Setup extends MockFactory {
    implicit val materializer: Materializer = mock[Materializer]

    implicit val messagesApi: MessagesApi = mock[MessagesApi]

    val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    val viewConfig: ViewConfig = ViewConfigMockFactory()

    val errorHandler = mock[ErrorHandler]
    val controller = new ChangeLanguageEndpoint(viewConfig, errorHandler)
  }

  private trait Mocks {
    self: Setup =>

    def expectRedirect(to: String, withLang: Lang) = new {
      def returning(result: Result) = {
        val redirectWithLang: Result = mock[Result]
        (redirectWithLang.flashing(_: Flash))
          .expects(FlashWithSwitchIndicator)
          .returning(result)

        (messagesApi.setLang(_: Result, _: Lang))
          .expects(redirect(to), withLang)
          .returning(redirectWithLang)
      }
    }

    private def redirect(url: String) = argAssert { (result: Result) =>
      result.header.status shouldBe SEE_OTHER
      result.header.headers(LOCATION) shouldBe url
    }
  }
}
