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

package uk.gov.hmrc.personaldetailsvalidationfrontend.helloworld

import akka.stream.Materializer
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, Request, RequestHeader}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.personaldetailsvalidationfrontend.config.ViewConfig
import uk.gov.hmrc.play.test.UnitSpec

class HelloWorldEndpointSpec
  extends UnitSpec
    with MockFactory
    with ScalaFutures {

  "hello-world" should {
    "return OK with page body returned from the given Page" in new Setup {
      (page.render(_: Request[_], _: Messages, _: ViewConfig))
        .expects(request, messages, viewConfig)
        .returning(Html("content"))

      val result = endpoint.helloWorld(request)

      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
      bodyOf(result).futureValue shouldBe "content"
    }
  }

  private trait Setup {
    private implicit val messagesApi: MessagesApi = mock[MessagesApi]
    implicit val messages: Messages = mock[Messages]
    implicit val request: Request[AnyContentAsEmpty.type] = FakeRequest("GET", "/")
    (messagesApi.preferred(_: RequestHeader)).expects(request).returning(messages)

    implicit val viewConfig: ViewConfig = mock[ViewConfig]
    implicit val materializer: Materializer = mock[Materializer]

    val page: HelloWorldPage = mock[HelloWorldPage]

    val endpoint = new HelloWorldEndpoint(page)
  }
}
