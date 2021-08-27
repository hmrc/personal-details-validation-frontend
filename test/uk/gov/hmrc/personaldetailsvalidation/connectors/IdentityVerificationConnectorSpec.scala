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

package uk.gov.hmrc.personaldetailsvalidation.connectors

import ch.qos.logback.classic.Level
import org.scalamock.scalatest.MockFactory
import org.scalatest.LoneElement.convertToCollectionLoneElementWrapper
import org.scalatest.concurrent.Eventually.eventually
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Writes
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import setups.LogCapturing
import setups.connectors.HttpClientStubSetup
import support.UnitSpec
import uk.gov.hmrc.config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}
import uk.gov.hmrc.personaldetailsvalidation.model.JourneyUpdate

import scala.concurrent.ExecutionContext.Implicits.{global => executionContext}
import scala.concurrent.{ExecutionContext, Future}

class IdentityVerificationConnectorSpec
  extends UnitSpec
    with GuiceOneAppPerSuite
    with MockFactory
    with LogCapturing {

  "IV Connector" should {
    "update the journey status in IV" in new Setup {
      (mockHttpClient.PATCH[JourneyUpdate, HttpResponse](_: String, _: JourneyUpdate, _: Seq[(String, String)])(_: Writes[JourneyUpdate], _: HttpReads[HttpResponse], _: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *, *, *, *).returning(Future.successful(HttpResponse(200, "")))
      withCaptureOfLoggingFrom(ivConnector.testLogger) { logEvents =>
        ivConnector.updateJourney(redirectingUrl)
        eventually {
          logEvents.count(_.getLevel == Level.WARN) shouldBe 0
        }
      }
    }

    "failed extract journeyId from redirecting url" in new Setup {

      withCaptureOfLoggingFrom(ivConnector.testLogger) { logEvents =>
        ivConnector.updateJourney("redirectingUrl")
          eventually {
            logEvents.filter(_.getLevel == Level.WARN).loneElement.getMessage should include(s"VER-333- cannot extract IV journeyId from redirecting url : = redirectingUrl")
          }
      }
    }
  }

  private trait Setup extends HttpClientStubSetup  {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

    val appConfig = app.injector.instanceOf[AppConfig]
    val mockHttpClient = mock[HttpClient]
    val redirectingUrl = "/mdtp/personal-details-validation-complete/261948fb-b807-4e5a-a5ca-3cdcc5009be4"

    val ivConnector = new IdentityVerificationConnector(appConfig, httpClient){
      val testLogger = logger
    }
  }
}
