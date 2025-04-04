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

package uk.gov.hmrc.personaldetailsvalidation.connectors

import ch.qos.logback.classic.Level
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.Eventually.eventually
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import setups.LogCapturing
import support.UnitSpec
import uk.gov.hmrc.config.AppConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.ExecutionContext.Implicits.{global => executionContext}

class IdentityVerificationConnectorSpec
  extends UnitSpec
    with GuiceOneAppPerSuite
    with MockFactory
    with LogCapturing {

  "IV Connector" should {

    "log a failure to extract journeyId from redirecting url" in new Setup {

      withCaptureOfLoggingFrom(ivConnector.testLogger, Level.WARN) { logEvents =>
        ivConnector.updateJourney("redirectingUrl", "Timeout")
          eventually {
            logEvents.filter(log => log.getLevel == Level.WARN && log.getMessage.contains("Cannot extract IV journeyId from redirecting url")).head.getMessage should include(s"Cannot extract IV journeyId from redirecting url")
          }
      }
    }
  }

  private trait Setup {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

    val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
    val mockHttpClient: HttpClientV2 = mock[HttpClientV2]

    val ivConnector = new IdentityVerificationConnector(appConfig, mockHttpClient){
      val testLogger = logger
    }
  }
}
