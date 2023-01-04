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

package uk.gov.hmrc.personaldetailsvalidation.monitoring.analytics

import akka.Done
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.Eventually
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Logger
import play.api.http.Status.OK
import play.api.libs.json.Writes
import setups.LogCapturing
import support.UnitSpec
import uk.gov.hmrc.config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AnalyticsConnectorSpec extends UnitSpec with Eventually with GuiceOneAppPerSuite with MockFactory with LogCapturing {

  "AnalyticsConnector" should {

    "send event" in new Setup {

      (mockHttpClient.POST[AnalyticsRequest, HttpResponse](_: String, _: AnalyticsRequest, _: Seq[(String, String)])(_: Writes[AnalyticsRequest],
        _: HttpReads[HttpResponse], _: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *, *, *, *)
        .returning(Future.successful(HttpResponse.apply(OK, "")))

      await(analyticsConnector.sendEvent(analyticsRequest)(hc, global)) shouldBe Done

    }

    "send event and log error if failed" in new Setup {

      withCaptureOfLoggingFrom(Logger("uk.gov.hmrc.personaldetailsvalidation.monitoring.analytics.AnalyticsConnector")) { events =>

        (mockHttpClient.POST[AnalyticsRequest, HttpResponse](_: String, _: AnalyticsRequest, _: Seq[(String, String)])(_: Writes[AnalyticsRequest],
          _: HttpReads[HttpResponse], _: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *, *, *, *)
          .returning(Future.failed(new RuntimeException))

        await(analyticsConnector.sendEvent(analyticsRequest)(hc, global)) shouldBe Done

        eventually {
          events.map(_.getMessage).head.contains("platform-analytics returns error java.lang.RuntimeException, might failed") shouldBe true
        }
      }

    }

  }

  private trait Setup {
    val gaClientId: String = "GA1.1.283183975.1456746121"
    val hc: HeaderCarrier = HeaderCarrier()

    val dimensions: Seq[DimensionValue] = Seq(DimensionValue(6, "unknown"))
    val analyticsRequest: AnalyticsRequest = AnalyticsRequest(Some(gaClientId), Seq(Event("sos_iv", "pdv", "test", dimensions)))

    val appConfg: AppConfig = app.injector.instanceOf[AppConfig]
    val mockHttpClient: HttpClient = mock[HttpClient]

    val analyticsConnector = new AnalyticsConnector(appConfg, mockHttpClient)
  }

}
