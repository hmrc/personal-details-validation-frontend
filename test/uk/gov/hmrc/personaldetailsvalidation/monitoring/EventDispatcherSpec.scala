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

package uk.gov.hmrc.personaldetailsvalidation.monitoring

import org.scalamock.scalatest.MockFactory
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import support.UnitSpec
import uk.gov.hmrc.config.AppConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.personaldetailsvalidation.monitoring.analytics.{AnalyticsConnector, AnalyticsEventHandler}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class EventDispatcherSpec extends UnitSpec with MockFactory {
  
  trait Setup {

    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    implicit val hc: HeaderCarrier = HeaderCarrier()
    
    var invoked: Boolean = false
    val mockAppConfg: AppConfig = mock[AppConfig]
    val mockAnalyticsConnector: AnalyticsConnector = mock[AnalyticsConnector]

    (mockAppConfg.analyticsToken _).expects().returns("UA-43414424-30")

    val workingEventDispatcher: AnalyticsEventHandler = new AnalyticsEventHandler(mockAppConfg, mockAnalyticsConnector) {
      override def handleEvent(event: MonitoringEvent)(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Unit =
        invoked = true
    }
  }

  "Event Dispatcher" should {
    "sends event successfully" in new Setup {
      val eventDispatcher = new EventDispatcher(workingEventDispatcher)
      eventDispatcher.dispatchEvent(TimeoutContinue())
      invoked shouldBe true
    }
  }
}
