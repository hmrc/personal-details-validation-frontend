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

package uk.gov.hmrc.personaldetailsvalidation.views.pages

import javax.inject.{Inject, Singleton}

import play.api.data.{Form, Mapping}
import play.api.data.Forms.mapping
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Request
import play.twirl.api.Html
import uk.gov.hmrc.config.AppConfig
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.personaldetailsvalidation.model._
import uk.gov.hmrc.personaldetailsvalidation.views.html.template._
import uk.gov.hmrc.views.ViewConfig

import scala.util.Try

@Singleton
private[personaldetailsvalidation] class PersonalDetailsPage @Inject()(appConfig: AppConfig) (implicit val messagesApi: MessagesApi,
                                                                       viewConfig: ViewConfig)
  extends I18nSupport {

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

  def render(postCodePageRequested: Boolean)(implicit completionUrl: CompletionUrl, request: Request[_]): Html =
    if (showPostCodePage(postCodePageRequested))
      personal_details_postcode(formWithPostcode, completionUrl)
    else
      personal_details_nino(formWithNino, completionUrl, appConfig.isPostCodeLookupEnabled)

  private def showPostCodePage(postCodePageRequested: Boolean) : Boolean = appConfig.isPostCodeLookupEnabled && postCodePageRequested

  def renderValidationFailure(postCodePageRequested: Boolean)(implicit completionUrl: CompletionUrl, request: Request[_]): Html =
    if (showPostCodePage(postCodePageRequested))
      personal_details_postcode(formWithPostcode.withGlobalError("personal-details.validation.failed"), completionUrl)
    else
      personal_details_nino(formWithNino.withGlobalError("personal-details.validation.failed"), completionUrl, appConfig.isPostCodeLookupEnabled)

  def bindFromRequest(postCodePageRequested: Boolean)(implicit request: Request[_],
                      completionUrl: CompletionUrl): Either[Html, PersonalDetails] =
    if(showPostCodePage(postCodePageRequested)) {
      formWithPostcode.bindFromRequest().fold(
        formWithErrors => Left(personal_details_postcode(formWithErrors, completionUrl)),
        personalDetails => Right(personalDetails)
      )
    } else {
      formWithNino.bindFromRequest().fold(
        formWithErrors => Left(personal_details_nino(formWithErrors, completionUrl, appConfig.isPostCodeLookupEnabled)),
        personalDetails => Right(personalDetails)
      )
    }
}
