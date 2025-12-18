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

package uk.gov.hmrc.errorhandling

import org.apache.pekko.stream.Materializer
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.Assertion
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.Helpers.*
import play.twirl.api.Html
import setups.views.ViewSetup
import support.UnitSpec
import uk.gov.hmrc.config.{AppConfig, DwpMessagesApiProvider}
import uk.gov.hmrc.errorhandling.ErrorHandler.bindingError
import uk.gov.hmrc.views.html.template.error_template

import scala.concurrent.ExecutionContext
import scala.reflect.Selectable.reflectiveSelectable

class ErrorHandlerSpec
  extends UnitSpec
    with GuiceOneAppPerSuite
    with ScalaFutures {

  "standardErrorTemplate" should {

    "error page with given title, heading and message" in new Setup {
      val html: Html = await(errorHandler.standardErrorTemplate("title", "heading", "error-message")(using request))

      val doc: Document = Jsoup.parse(html.toString())

      doc.title() shouldBe "title"
      doc.select("h1").text() shouldBe "heading"
      doc.select("p").get(0).text() shouldBe "error-message"
    }
  }

  "onClientError" should {

    s"return BAD_REQUEST status with the Technical Problem page " +
      s"for BAD_REQUEST status and message not containing $bindingError" in new Setup {
      val result: Result = errorHandler.onClientError(request, BAD_REQUEST, "error-message").futureValue

      status(result) shouldBe BAD_REQUEST
      verify(result).containsTechnicalErrorPage
    }

    s"return NOT_FOUND status with the Technical Problem page " +
      s"for BAD_REQUEST status and message containing $bindingError" in new Setup {
      val result: Result = errorHandler.onClientError(request, BAD_REQUEST, s"${bindingError}error-message").futureValue

      status(result) shouldBe NOT_FOUND
      verify(result).containsTechnicalErrorPage
    }

    s"return the given status with the Technical Problem page " +
      s"for non-BAD_REQUEST status" in new Setup {
      Set(NOT_FOUND, BAD_GATEWAY, INTERNAL_SERVER_ERROR) foreach { errorStatus =>

        val result = errorHandler.onClientError(request, errorStatus, "error-message").futureValue

        status(result) shouldBe errorStatus
        verify(result).containsTechnicalErrorPage
      }
    }
  }

  private trait Setup extends ViewSetup {

    implicit val materializer: Materializer = app.materializer
    implicit val ec: ExecutionContext = ExecutionContext.global
    implicit val dwpMessagesApi: DwpMessagesApiProvider = app.injector.instanceOf[DwpMessagesApiProvider]

    private val appConfig = app.injector.instanceOf[AppConfig]

    val errorHandler: ErrorHandler = new ErrorHandler(appConfig, app.injector.instanceOf[error_template])

    def verify(result: Result): Object {val containsTechnicalErrorPage: Assertion} = new {
      lazy val containsTechnicalErrorPage = {
        val html: Document = Html(bodyOf(result))

        html.title() shouldBe Messages("global.error.InternalServerError500.title")
        html.select("h1").text() shouldBe Messages("global.error.InternalServerError500.heading")
        html.select("p").get(0).text() shouldBe Messages("global.error.InternalServerError500.message")
      }
    }
  }
}
