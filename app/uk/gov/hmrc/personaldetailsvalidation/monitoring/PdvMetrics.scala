/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject._

import com.kenshoo.play.metrics.{DisabledMetrics, Metrics}
import uk.gov.hmrc.personaldetailsvalidation.model._

class PdvMetrics @Inject()(metrics: Metrics) {
  /**
    * increment the GA metrics counter to note whether we are trying to match against Nino or Post Code
    * @param details The personal details of the person we are trying to match
    * @return `true` if we managed to update the counter, otherwise `false`
    */
  def matchPersonalDetails(details: PersonalDetails) : Boolean = {
    val counterName = details match {
      case _ : PersonalDetailsWithNino => "match-user.nino"
      case _ : PersonalDetailsWithPostcode => "match-user.postcode"
    }

    metrics match {
      case _ : DisabledMetrics =>
        false
      case _ =>
        metrics.defaultRegistry.counter(counterName).inc()
        true
    }
  }
}
