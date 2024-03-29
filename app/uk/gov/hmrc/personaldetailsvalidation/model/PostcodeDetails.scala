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

import play.api.data.Forms.mapping
import play.api.data.{Form, Mapping}
import uk.gov.hmrc.formmappings.Mappings.mandatoryText

case class PostcodeDetails(postcode: NonEmptyString)

object PostcodeDetailsForm {

  private def postcodeValidation(): Mapping[NonEmptyString] = {
    mandatoryText("personal-details.postcode.invalid").
      verifying("personal-details.postcode.invalid", postcodeFormatValidation _)
  }

  def postcodeFormatValidation(postcode: NonEmptyString): Boolean =
    postcode.value.matches("""([A-Za-z]\s*[A-HJ-Ya-hj-y]?\s*[0-9]\s*[A-Za-z0-9]?|[A-Za-z]\s*[A-HJ-Ya-hj-y]\s*[A-Za-z])\s*[0-9]\s*([ABDEFGHJLNPQRSTUWXYZabdefghjlnpqrstuwxyz]\s*){2}""")

  val postcodeForm: Form[PostcodeDetails] = Form(mapping(
    "postcode" -> postcodeValidation()
  )(PostcodeDetails.apply)(PostcodeDetails.unapply))

}
