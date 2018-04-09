/*
 * Copyright 2018 HM Revenue & Customs
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

import com.codahale.metrics.{Counter, MetricRegistry}
import uk.gov.hmrc.play.test.UnitSpec
import com.kenshoo.play.metrics.{DisabledMetrics, Metrics}
import uk.gov.hmrc.personaldetailsvalidation.generators.ObjectGenerators._

class PdvMetricsSpec extends UnitSpec {
  val WITH_NINO_COUNTER = "match-user.nino"
  val WITH_POSTCODE_COUNTER = "match-user.postcode"

  "PdvMetric" should {
    "increment the count when a Nino is used in the designated details page" in new Setup {
      val countBefore = mockedRegistry.counter(WITH_NINO_COUNTER).getCount

      pdvMetrics.matchPersonalDetails(detailsWithNino)

      mockedRegistry.counter(WITH_NINO_COUNTER).getCount shouldBe countBefore + 1
    }

    "increment the count when a Postcode is used in the designated details page" in new Setup {
      val countBefore = mockedRegistry.counter(WITH_POSTCODE_COUNTER).getCount

      pdvMetrics.matchPersonalDetails(detailsWithPostcode)

      mockedRegistry.counter(WITH_POSTCODE_COUNTER).getCount shouldBe countBefore + 1
    }

    "not increment any counters if the metrics are disbaled and using a Nino" in new Setup {
      pdvWithDisabledMetrics.matchPersonalDetails(detailsWithNino)
    }

    "not increment any counters if the metrics are disbaled and using a Postcode" in new Setup {
      pdvWithDisabledMetrics.matchPersonalDetails(detailsWithPostcode)
    }
  }

  // ScalaMock does not currently handle classes.
  private trait Setup {
    val detailsWithNino = personalDetailsObjects.sample.get
    val detailsWithPostcode = personalDetailsObjectsWithPostcode.sample.get

    val mockedMetrics : Metrics = new MockMetrics
    val mockedRegistry : MetricRegistry = new MetricRegistry()
    val pdvMetrics = new PdvMetrics(mockedMetrics)
    val pdvWithDisabledMetrics = new PdvMetrics(new DisabledMetrics)

    private class MockMetrics extends Metrics {
      override def defaultRegistry: MetricRegistry = mockedRegistry

      override def toJson: String = ???
    }

    private class MockCounter extends Counter {
      private var count: Int = 0
      override def inc(): Unit = count += 1
      override def getCount: Long = count
    }

    private class MockMetricRegistry extends MetricRegistry {
      private var counters = Map[String, MockCounter]()
      override def counter(name: String): Counter = {
        counters.get(name) match {
          case Some(counter) =>
            counter
          case None =>
            var newCounter = new MockCounter
            counters + (name -> newCounter)
            newCounter
        }
      }
    }
  }
}
