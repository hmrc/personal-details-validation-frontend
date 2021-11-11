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

package uk.gov.hmrc.personaldetailsvalidation.endpoints

import akka.stream.Materializer
import cats.Id
import com.kenshoo.play.metrics.Metrics
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import support.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.logging.Logger
import uk.gov.hmrc.personaldetailsvalidation.connectors.PersonalDetailsSender
import uk.gov.hmrc.personaldetailsvalidation.model._
import uk.gov.hmrc.personaldetailsvalidation.monitoring.PdvMetrics

class PersonalDetailsSubmissionSpec extends UnitSpec with MockFactory with GuiceOneAppPerSuite with ScalaCheckDrivenPropertyChecks {

  trait Setup {
    implicit val request: Request[AnyContentAsEmpty.type] = FakeRequest()
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders(("origin", origin))
    implicit val materializer: Materializer = app.materializer

    val origin: String = "Unknown-Origin"

    val personalDetailsValidationConnector = mock[PersonalDetailsSender[Id]]
    val logger = mock[Logger]
    val metrics = mock[Metrics]
    val pdvMetrics = new MockPdvMetrics

    class MockPdvMetrics extends PdvMetrics(metrics) {
      var ninoCounter = 0
      var postCodeCounter = 0
      var errorCounter = 0
      override def matchPersonalDetails(details: PersonalDetails): Boolean = {
        details match {
          case _ : PersonalDetailsWithNino =>
            ninoCounter += 1
            true
          case _ : PersonalDetailsWithPostcode =>
            postCodeCounter += 1
            true
          case _ =>
            errorCounter += 1
            false
        }
      }
    }

    new PersonalDetailsSubmission[Id](personalDetailsValidationConnector, pdvMetrics, logger)
  }
}
