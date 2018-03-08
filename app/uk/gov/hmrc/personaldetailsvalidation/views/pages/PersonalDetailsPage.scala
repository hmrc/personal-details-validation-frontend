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

package uk.gov.hmrc.personaldetailsvalidation.views.pages

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Request
import play.twirl.api.Html
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.personaldetailsvalidation.model._
import uk.gov.hmrc.personaldetailsvalidation.views.html.template.personal_details
import uk.gov.hmrc.views.ViewConfig

import scala.util.Try

@Singleton
private[personaldetailsvalidation] class PersonalDetailsPage @Inject()(implicit val messagesApi: MessagesApi,
                                                                       viewConfig: ViewConfig)
  extends I18nSupport {

  import uk.gov.hmrc.formmappings.Mappings._

  private val form: Form[PersonalDetails] = Form(mapping(
    "firstName" -> mandatoryText("personal-details.firstname.required"),
    "lastName" -> mandatoryText("personal-details.lastname.required"),
    "nino" -> optionalText
      .verifying("personal-details.nino.invalid", nonEmptyString => nonEmptyString.forall(nonEmptyString => Try(Nino(nonEmptyString.value)).isSuccess))
      .transform[Option[Nino]](_.map(nonEmptyString => Nino(nonEmptyString.value)), _.map(nino => NonEmptyString(nino.value))),
    "dateOfBirth" -> mandatoryLocalDate("personal-details"),
    "postcode" -> optionalText
      .verifying("personal-details.postcode.invalid", nonEmptyString => nonEmptyString.forall(nonEmptyString => Try(nonEmptyString.value).isSuccess))
  )(personalDetailsParser)(Some(_))
    .verifying("personal-details.ninoOrPostcode.required", ninoAndPostcodeMutuallyExclusive _)
    .transform(createPersonalDetails, createPersonalDetailsData)
  )

  type PersonalDetailsData = Tuple5[NonEmptyString,NonEmptyString,Option[Nino],LocalDate,Option[NonEmptyString]]

  private lazy val personalDetailsParser: (NonEmptyString,NonEmptyString,Option[Nino],LocalDate,Option[NonEmptyString]) => PersonalDetailsData =
    (_,_,_,_,_)

  private def createPersonalDetails(personalDetailsData: PersonalDetailsData): PersonalDetails = {
    val (firstName, lastName, mayBeNino, dateOfBirth, mayBePostcode) = personalDetailsData
    (mayBeNino, mayBePostcode) match {
      case (Some(nino), None) => PersonalDetailsWithNino(firstName, lastName, nino, dateOfBirth)
      case (None, Some(postcode)) => PersonalDetailsWithPostcode(firstName, lastName, postcode, dateOfBirth)
      case _ => throw new IllegalStateException("Either of nino or postcode should be present")
    }
  }

  private def createPersonalDetailsData(personalDetails: PersonalDetails): PersonalDetailsData = personalDetails match {
    case PersonalDetailsWithNino(firstName, lastName, nino, dateOfBirth) => (firstName, lastName, Some(nino), dateOfBirth, None)
    case PersonalDetailsWithPostcode(firstName, lastName, postcode, dateOfBirth) => (firstName, lastName, None, dateOfBirth, Some(postcode))
  }

  private def ninoAndPostcodeMutuallyExclusive(personalDetailsData: PersonalDetailsData): Boolean = {
    val (_, _, nino, _, postcode) = personalDetailsData
    (nino, postcode) match {
      case (Some(_), Some(_)) => false
      case (None, None) => false
      case _ => true
    }
  }

  def render(implicit completionUrl: CompletionUrl,
             request: Request[_]): Html =
    personal_details(form, completionUrl)

  def bindFromRequest(implicit request: Request[_],
                      completionUrl: CompletionUrl): Either[Html, PersonalDetails] =
    form.bindFromRequest().fold(
      formWithErrors => Left(personal_details(formWithErrors, completionUrl)),
      personalDetails => Right(personalDetails)
    )
}
