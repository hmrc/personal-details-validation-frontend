/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.personaldetailsvalidation.utils

import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.personaldetailsvalidation.model.{PersonalDetails, PersonalDetailsWithNino, PersonalDetailsWithPostcode}

object PersonalDetailsHelper {

  implicit val personalDetailsWrites: Writes[PersonalDetails] = Writes[PersonalDetails] {
    case personalDetails: PersonalDetailsWithNino =>
      Json.obj(
        "firstName" -> personalDetails.firstName,
        "lastName" -> personalDetails.lastName,
        "dateOfBirth" -> personalDetails.dateOfBirth,
        "nino" -> personalDetails.nino
      )
    case personalDetails: PersonalDetailsWithPostcode =>
      Json.obj(
        "firstName" -> personalDetails.firstName,
        "lastName" -> personalDetails.lastName,
        "dateOfBirth" -> personalDetails.dateOfBirth,
        "postCode" -> personalDetails.postCode
      )
  }

}
