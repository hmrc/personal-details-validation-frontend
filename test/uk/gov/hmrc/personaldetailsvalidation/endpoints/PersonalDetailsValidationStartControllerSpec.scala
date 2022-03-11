/*
 * Copyright 2022 HM Revenue & Customs
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

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.Results.Redirect
import play.api.mvc._
import play.api.test.FakeRequest
import support.Generators.Implicits._
import support.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators
import uk.gov.hmrc.personaldetailsvalidation.model.CompletionUrl

import scala.concurrent.Future

class PersonalDetailsValidationStartControllerSpec extends UnitSpec with MockFactory with ScalaFutures with GuiceOneAppPerSuite {

  "start" should {

    "fetch redirect from the JourneyStart (when user call /start)" in new Setup {

      val redirect: Result = Redirect("some-url")

      (journeyStart.findRedirect(_: CompletionUrl, _:Option[String], _: Option[CompletionUrl])(_: Request[_], _: HeaderCarrier))
        .expects(url, origin, *, *, *)
        .returning(Future.successful(redirect))

      controller.start(url, origin, failureUrl)(request).futureValue shouldBe redirect
    }

    "throw an exception when fetchRedirect returns one" in new Setup {

      (journeyStart.findRedirect(_: CompletionUrl, _:Option[String], _: Option[CompletionUrl])(_: Request[_], _: HeaderCarrier))
        .expects(url, origin, *, *, *)
        .returning(Future.failed(new RuntimeException("Unrecoverable error")))

      a[RuntimeException] should be thrownBy controller.start(url, origin, failureUrl)(request).futureValue
    }
  }

  private trait Setup {

    protected implicit val request: Request[AnyContentAsEmpty.type] = FakeRequest()

    val journeyStart: JourneyStart = mock[JourneyStart]

    val url: CompletionUrl = ValuesGenerators.completionUrls.generateOne
    val failureUrl: Option[CompletionUrl] = None

    val origin: Option[String] = Some("test")

    def stubMessagesControllerComponents() : MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

    val controller = new PersonalDetailsValidationStartController(journeyStart, stubMessagesControllerComponents())
  }
}
