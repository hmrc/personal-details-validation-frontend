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

package uk.gov.hmrc.personaldetailsvalidation.endpoints

import generators.Generators.Implicits._
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.mvc.Results.Redirect
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.{stubBodyParser, stubControllerComponents}
import scalamock.AsyncMockArgumentMatchers
import support.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators
import uk.gov.hmrc.personaldetailsvalidation.model.CompletionUrl

import scala.concurrent.Future

class PersonalDetailsValidationStartControllerSpec
  extends UnitSpec
    with AsyncMockFactory
    with AsyncMockArgumentMatchers
    with ScalaFutures {

  "start" should {

    "fetch redirect from the JourneyStart" in new Setup {

      val redirect: Result = Redirect("some-url")

      (journeyStart.findRedirect(_: CompletionUrl)(_: Request[_], _: HeaderCarrier))
        .expects(url, request, instanceOf[HeaderCarrier])
        .returning(Future.successful(redirect))

      controller.start(url)(request).futureValue shouldBe redirect
    }

    "throw an exception when fetchRedirect returns one" in new Setup {

      (journeyStart.findRedirect(_: CompletionUrl)(_: Request[_], _: HeaderCarrier))
        .expects(url, request, instanceOf[HeaderCarrier])
        .returning(Future.failed(new RuntimeException("Unrecoverable error")))

      a[RuntimeException] should be thrownBy controller.start(url)(request).futureValue
    }
  }

  private trait Setup {

    protected implicit val request: Request[AnyContentAsEmpty.type] = FakeRequest()

    val journeyStart = mock[FuturedJourneyStart]

    val url = ValuesGenerators.completionUrls.generateOne

    def stubMessagesControllerComponents() : MessagesControllerComponents = {
      val stub = stubControllerComponents()
      DefaultMessagesControllerComponents(
        new DefaultMessagesActionBuilderImpl(stubBodyParser(AnyContentAsEmpty),stub.messagesApi)(stub.executionContext),
        DefaultActionBuilder(stub.actionBuilder.parser)(stub.executionContext), stub.parsers, stub.messagesApi, stub.langs, stub.fileMimeTypes,
        stub.executionContext
      )
    }

    val controller = new PersonalDetailsValidationStartController(journeyStart, stubMessagesControllerComponents())
  }
}
