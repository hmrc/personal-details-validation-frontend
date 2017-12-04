/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.personaldetailsvalidationfrontend.model

import java.util.UUID

import cats.data.Validated
import uk.gov.voa.valuetype.StringValue

case class JourneyId private(validated: Validated[IllegalArgumentException, String])
  extends StringValue {
  override val value: String = validated.fold(throw _, identity)
}

object JourneyId {
  def apply(value: String): JourneyId = JourneyId(JourneyIdValueValidator(value))
}

object JourneyIdValueValidator extends ((String) => Validated[IllegalArgumentException, String]) {
  def apply(value: String): Validated[IllegalArgumentException, String] =
    Validated.catchOnly[IllegalArgumentException] {
      UUID.fromString(value)
      value
    }
}
