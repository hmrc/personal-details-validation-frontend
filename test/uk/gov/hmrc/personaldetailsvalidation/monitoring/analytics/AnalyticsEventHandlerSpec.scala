/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.personaldetailsvalidation.monitoring.analytics

import akka.Done
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.Eventually
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{AnyContentAsEmpty, Cookie}
import play.api.test.FakeRequest
import support.UnitSpec
import uk.gov.hmrc.config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.personaldetailsvalidation.monitoring.{EventDispatcher, TimedOut, TimeoutContinue}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AnalyticsEventHandlerSpec extends UnitSpec with Eventually with GuiceOneAppPerSuite with MockFactory {

  "dispatcher" should {

    val dimensions: Seq[DimensionValue] = Seq(DimensionValue(6, "unknown"))

    "send pdv_modal_timeout continue event when user clicks on stay signed in " in new Setup {
      dispatcher.dispatchEvent(TimeoutContinue)(request, hc, global)
      eventually {
        analyticsRequests.head shouldBe AnalyticsRequest(Some(gaClientId), Seq(
          Event("sos_iv", "pdv_modal_timeout", "continue", dimensions)))
      }
    }

    "send pdv_modal_timeout ends event when user is not responded " in new Setup {
      dispatcher.dispatchEvent(TimedOut)(request, hc, global)
      eventually {
        analyticsRequests.head shouldBe AnalyticsRequest(Some(gaClientId), Seq(
          Event("sos_iv", "pdv_modal_timeout", "end", dimensions)))
      }
    }

    "check sessionId and clientId before sending pdv_modal_timeout event " in new Setup {
      dispatcher.dispatchEvent(TimedOut)(FakeRequest(), hc, global)
      eventually {
        analyticsRequests shouldBe List.empty[AnalyticsRequest]
      }
    }
  }

  private trait Setup {
    val gaClientId: String = "GA1.1.283183975.1456746121"
    val hc: HeaderCarrier = HeaderCarrier()
    var analyticsRequests: Seq[AnalyticsRequest] = Seq.empty
    val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withCookies(Cookie("_ga", gaClientId))

    val appConfg: AppConfig = app.injector.instanceOf[AppConfig]
    val mockHttpClient: HttpClient = mock[HttpClient]

    object TestConnector extends AnalyticsConnector(appConfg, mockHttpClient) {
      override def sendEvent(request: AnalyticsRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Done] = {
        analyticsRequests = analyticsRequests :+ request
        Future.successful(Done)
      }
    }

    object TestHandler extends AnalyticsEventHandler(appConfg, TestConnector)

    val dispatcher = new EventDispatcher(TestHandler)

  }

}
