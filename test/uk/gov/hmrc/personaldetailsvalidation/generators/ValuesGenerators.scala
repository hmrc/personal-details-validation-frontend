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

package uk.gov.hmrc.personaldetailsvalidation.generators

import org.scalacheck.Gen
import uk.gov.hmrc.personaldetailsvalidation.model.RelativeUrl.relativeUrl
import uk.gov.hmrc.personaldetailsvalidation.model.{JourneyId, RelativeUrl}

object ValuesGenerators {

  import Generators._

  implicit val journeyIds: Gen[JourneyId] = Gen.uuid map JourneyId.apply
  implicit val relativeUrls: Gen[RelativeUrl] = nonEmptyStrings map { string =>
    relativeUrl(s"/$string").fold(throw _, identity)
  }
}