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

import org.jsoup.nodes.Document
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n._
import play.api.test.FakeRequest
import uk.gov.hmrc.personaldetailsvalidationfrontend.views.ViewSetup
import uk.gov.hmrc.play.test.UnitSpec

class ErrorHandlerSpec
  extends UnitSpec
    with OneAppPerSuite {

  "standardErrorTemplate" should {

    "error page with given title, heading and message" in new Setup {
      val html: Document = errorHandler.standardErrorTemplate("title", "heading", "error-message")(request)

      html.title() shouldBe "title"
      html.select("header h1").text() shouldBe "heading"
      html.select("header ~ p").first().text() shouldBe "error-message"
    }
  }

  private trait Setup extends ViewSetup {
    val request = FakeRequest()

    implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

    val errorHandler: ErrorHandler = new ErrorHandler()
  }
}
