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

package uk.gov.hmrc.personaldetailsvalidation.model

import play.api.data.Form
import play.api.data.Forms.mapping
import uk.gov.hmrc.formmappings.Mappings.{mandatoryLocalDate, mandatoryText}

import java.time.LocalDate


case class InitialPersonalDetails(firstName: NonEmptyString,
                                  lastName: NonEmptyString,
                                  dateOfBirth: LocalDate)

object InitialPersonalDetailsForm {
  val initialForm: Form[InitialPersonalDetails] = Form(mapping(
    "firstName" -> mandatoryText("personal-details.firstname.required"),
    "lastName" -> mandatoryText("personal-details.lastname.required"),
    "dateOfBirth" -> mandatoryLocalDate("personal-details")
  )(InitialPersonalDetails.apply)(formData => Some(Tuple.fromProductTyped(formData.firstName, formData.lastName, formData.dateOfBirth))))
}
