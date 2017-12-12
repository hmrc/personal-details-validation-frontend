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

package setups.controllers

import akka.stream.Materializer
import org.scalamock.scalatest.MockFactory
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, RequestHeader, Result}
import play.api.test.FakeRequest

import scala.concurrent.Future

trait EndpointSetup extends MockFactory {
  protected implicit val messagesApi: MessagesApi = mock[MessagesApi]
  protected implicit val messages: Messages = mock[Messages]

  protected implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  (messagesApi.preferred(_: RequestHeader)).expects(request).returning(messages)
}

trait EndpointRequiringBodySetup extends EndpointSetup {

  import org.scalatest.Matchers._
  import play.api.test.Helpers._

  protected implicit val materializer: Materializer = mock[Materializer]

  def verify(result: Future[Result])
            (implicit materializer: Materializer) = new {

    def has(statusCode: Int, content: String): Unit = {
      status(result) shouldBe statusCode
      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")
      contentAsString(result) shouldBe content
    }
  }
}