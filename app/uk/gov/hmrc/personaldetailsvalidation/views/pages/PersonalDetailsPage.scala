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

package uk.gov.hmrc.personaldetailsvalidation.views.pages

import javax.inject.{Inject, Singleton}
import play.api.data.Forms.mapping
import play.api.data.{Form, Mapping}
import play.api.i18n.MessagesApi
import play.api.mvc.Request
import play.twirl.api.Html
import uk.gov.hmrc.config.{AppConfig, DwpMessagesApiProvider}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.language.DwpI18nSupport
import uk.gov.hmrc.personaldetailsvalidation.model._
import uk.gov.hmrc.personaldetailsvalidation.views.html.template._
import uk.gov.hmrc.views.ViewConfig

import scala.util.Try

@Singleton
private[personaldetailsvalidation]
class PersonalDetailsPage @Inject()(
        appConfig: AppConfig,
        personalDetailsPostcode: personal_details_postcode,
        personalDetailsNino: personal_details_nino
   )(implicit val dwpMessagesApiProvider: DwpMessagesApiProvider,
                                    viewConfig: ViewConfig,
     messageApi: MessagesApi)
  extends DwpI18nSupport(appConfig, messageApi) {

  override implicit lazy val messagesApi: MessagesApi = dwpMessagesApiProvider.get

  import uk.gov.hmrc.formmappings.Mappings._

  private val formWithNino: Form[PersonalDetailsWithNino] = Form(mapping(
    "firstName" -> mandatoryText("personal-details.firstname.required"),
    "lastName" -> mandatoryText("personal-details.lastname.required"),
    "nino" -> ninoValidation(),
    "dateOfBirth" -> mandatoryLocalDate("personal-details")
  )(PersonalDetailsWithNino.apply)(PersonalDetailsWithNino.unapply))

  private val formWithPostcode: Form[PersonalDetailsWithPostcode] = Form(mapping(
    "firstName" -> mandatoryText("personal-details.firstname.required"),
    "lastName" -> mandatoryText("personal-details.lastname.required"),
    "postcode" -> postcodeValidation(),
    "dateOfBirth" -> mandatoryLocalDate("personal-details")
  )(PersonalDetailsWithPostcode.apply)(PersonalDetailsWithPostcode.unapply))

  private def ninoValidation(): Mapping[Nino] = {
    mandatoryText("personal-details.nino.required")
      .verifying("personal-details.nino.invalid", nonEmptyString => Try(Nino(nonEmptyString.value.toUpperCase)).isSuccess)
      .transform[Nino](validatedNonEmptyNino => Nino(validatedNonEmptyNino.value.toUpperCase), nino => NonEmptyString(nino.toString.toUpperCase))
  }

  private def postcodeValidation(): Mapping[NonEmptyString] = {
    mandatoryText("personal-details.postcode.invalid").
      verifying("personal-details.postcode.invalid", postcodeFormatValidation _)
  }

  private def postcodeFormatValidation(postcode: NonEmptyString) =
    postcode.value.matches("""([A-Za-z][A-HJ-Ya-hj-y]?[0-9][A-Za-z0-9]?|[A-Za-z][A-HJ-Ya-hj-y][A-Za-z])\s?[0-9][ABDEFGHJLNPQRSTUWXYZabdefghjlnpqrstuwxyz]{2}""")

  def render(postCodePageRequested: Boolean)(implicit completionUrl: CompletionUrl, request: Request[_]): Html = {
    if (postCodePageRequested)
      personalDetailsPostcode(formWithPostcode, completionUrl)
    else
      personalDetailsNino(formWithNino, completionUrl)
  }

  def renderValidationFailure(postCodePageRequested: Boolean)(implicit completionUrl: CompletionUrl, request: Request[_]): Html =
    if (postCodePageRequested)
      personalDetailsPostcode(formWithPostcode.withGlobalError("personal-details.validation.failed"), completionUrl)
    else
      personalDetailsNino(formWithNino.withGlobalError("personal-details.validation.failed"), completionUrl)

  def bindFromRequest(postCodePageRequested: Boolean)(implicit request: Request[_],
                      completionUrl: CompletionUrl): Either[Html, PersonalDetails] = {
    import play.api.data.FormBinding.Implicits._
    if(postCodePageRequested) {
      formWithPostcode.bindFromRequest().fold(
        formWithErrors => Left(personalDetailsPostcode(formWithErrors, completionUrl)),
        personalDetails => Right(personalDetails)
      )
    } else {
      formWithNino.bindFromRequest().fold(
        formWithErrors => Left(personalDetailsNino(formWithErrors, completionUrl)),
        personalDetails => Right(personalDetails)
      )
    }
  }
}
