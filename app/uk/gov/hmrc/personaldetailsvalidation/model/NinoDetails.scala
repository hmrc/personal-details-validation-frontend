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
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.formmappings.Mappings.mandatoryText

import scala.util.Try

case class NinoDetails(nino: Nino)

object NinoDetailsForm {

  private def ninoValidation(): Mapping[Nino] = {
    mandatoryText("personal-details.nino.required")
      .verifying("personal-details.nino.invalid", nonEmptyString => Try(Nino(nonEmptyString.value.replace(" ", "").toUpperCase)).isSuccess)
      .transform[Nino](validatedNonEmptyNino => Nino(validatedNonEmptyNino.value.replace(" ", "").toUpperCase), nino => NonEmptyString(nino.toString.toUpperCase))
  }

  val ninoForm: Form[NinoDetails] = Form(mapping(
    "nino" -> ninoValidation()
  )(NinoDetails.apply)(pd => Some(pd.nino)))
}
