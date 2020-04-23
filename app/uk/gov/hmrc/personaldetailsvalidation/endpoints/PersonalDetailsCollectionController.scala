/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.personaldetailsvalidation.endpoints

import cats.implicits._
import javax.inject.Inject
import play.api.data.{Form, Mapping}
import play.api.data.Forms.mapping
import play.api.i18n.Messages
import play.api.mvc._
import uk.gov.hmrc.config.{AppConfig, DwpMessagesApi}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.personaldetailsvalidation.model._
import uk.gov.hmrc.personaldetailsvalidation.views.pages.PersonalDetailsPage
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.personaldetailsvalidation.views.html.template._
import uk.gov.hmrc.views.ViewConfig
import java.time.LocalDate

import cats.data.EitherT
import uk.gov.hmrc.language.DwpI18nSupport

import scala.concurrent.Future
import scala.util.Try

class PersonalDetailsCollectionController @Inject()(page: PersonalDetailsPage,
                                                    personalDetailsSubmission: FuturedPersonalDetailsSubmission,
                                                    appConfig: AppConfig)(implicit val dwpMessagesApi: DwpMessagesApi, viewConfig: ViewConfig)
  extends DwpI18nSupport(appConfig) with FrontendController {

  import uk.gov.hmrc.formmappings.Mappings._

  private final val FIRST_NAME_KEY = "firstName"
  private final val LAST_NAME_KEY = "lastName"
  private final val DOB_KEY = "dob"

  private val initialForm: Form[InitialPersonalDetails] = Form(mapping(
    "firstName" -> mandatoryText("personal-details.firstname.required"),
    "lastName" -> mandatoryText("personal-details.lastname.required"),
    "dateOfBirth" -> mandatoryLocalDate("personal-details")
  )(InitialPersonalDetails.apply)(InitialPersonalDetails.unapply))

  private def ninoValidation(): Mapping[Nino] = {
    mandatoryText("personal-details.nino.required")
      .verifying("personal-details.nino.invalid", nonEmptyString => Try(Nino(nonEmptyString.value.toUpperCase)).isSuccess)
      .transform[Nino](validatedNonEmptyNino => Nino(validatedNonEmptyNino.value.toUpperCase), nino => NonEmptyString(nino.toString.toUpperCase))
  }

  private val pdvSessionKeys : List[String] = List("firstName", "lastName", "dob")

  private val ninoForm: Form[NinoDetails] = Form(mapping(
    "nino" -> ninoValidation()
  )(NinoDetails.apply)(NinoDetails.unapply))

  private val postcodeForm: Form[PostcodeDetails] = Form(mapping(
    "postcode" -> postcodeValidation()
  )(PostcodeDetails.apply)(PostcodeDetails.unapply))

  private def postcodeValidation(): Mapping[NonEmptyString] = {
    mandatoryText("personal-details.postcode.invalid").
      verifying("personal-details.postcode.invalid", postcodeFormatValidation _)
  }

  private def postcodeFormatValidation(postcode: NonEmptyString) =
    postcode.value.matches("""([A-Za-z][A-HJ-Ya-hj-y]?[0-9][A-Za-z0-9]?|[A-Za-z][A-HJ-Ya-hj-y][A-Za-z])\s?[0-9][ABDEFGHJLNPQRSTUWXYZabdefghjlnpqrstuwxyz]{2}""")

  def showPage(implicit completionUrl: CompletionUrl, alternativeVersion: Boolean): Action[AnyContent] = Action { implicit request =>
    if (appConfig.isMultiPageEnabled) {
      val form: Form[InitialPersonalDetails] = retrieveMainDetails match {
        case (Some(firstName), Some(lastName), Some(dob)) =>
          val pd = InitialPersonalDetails(NonEmptyString(firstName), NonEmptyString(lastName), LocalDate.parse(dob))
          initialForm.fill(pd)
        case _ => initialForm
      }
      Ok(personal_details_main(form, completionUrl))
    } else {
      Ok(page.render(alternativeVersion))
    }
  }

  def submitMainDetails(completionUrl: CompletionUrl): Action[AnyContent] = Action.async { implicit request =>
    initialForm.bindFromRequest().fold (
      formWithErrors => Future.successful(Ok(personal_details_main(formWithErrors, completionUrl))),
      mainDetails => {
        val updatedSessionData = request.session.data ++ Map(
          FIRST_NAME_KEY -> mainDetails.firstName.value,
          LAST_NAME_KEY -> mainDetails.lastName.value,
          DOB_KEY -> mainDetails.dateOfBirth.toString
        )
        Future.successful(Redirect(routes.PersonalDetailsCollectionController.showNinoForm(completionUrl))
          .withSession(Session(updatedSessionData))
        )
      }
    )
  }

  def showNinoForm(completionUrl: CompletionUrl) = Action.async { implicit request =>
    if (hasMainDetailsAndIsMultiPage)
      Future.successful(Ok(enter_your_details_nino(ninoForm, completionUrl)))
    else
      Future.successful(Redirect(routes.PersonalDetailsCollectionController.showPage(completionUrl, false)))
  }

  def submitNino(completionUrl: CompletionUrl) = Action.async { implicit request =>
    if (appConfig.isMultiPageEnabled) {
      ninoForm.bindFromRequest().fold(
        formWithErrors => Future.successful(Ok(enter_your_details_nino(formWithErrors, completionUrl))),
        ninoForm => {
          retrieveMainDetails match {
            case (Some(fn), Some(ln), Some(dob)) =>
              val personalDetails = PersonalDetailsWithNino(NonEmptyString(fn), NonEmptyString(ln), ninoForm.nino, LocalDate.parse(dob))
              submitPersonalDetails(personalDetails, completionUrl)
            case _ => EitherT.rightT[Future, Result](BadRequest)
          }
        }.merge
      )
    } else {
      Future.successful(BadRequest)
    }
  }

  def showPostCodeForm(completionUrl: CompletionUrl) = Action.async { implicit request =>
    if (hasMainDetailsAndIsMultiPage)
      Future.successful(Ok(enter_your_details_postcode(postcodeForm, completionUrl)))
    else
      Future.successful(Redirect(routes.PersonalDetailsCollectionController.showPage(completionUrl, false)))
  }

  def submitPostcode(completionUrl: CompletionUrl) = Action.async { implicit request =>
    if (appConfig.isMultiPageEnabled) {
      postcodeForm.bindFromRequest().fold (
        formWithErrors => Future.successful(Ok(enter_your_details_postcode(formWithErrors, completionUrl))),
        postCodeForm => {
          retrieveMainDetails match {
            case (Some(fn), Some(ln), Some(dob)) =>
              val personalDetails = PersonalDetailsWithPostcode(NonEmptyString(fn), NonEmptyString(ln),postCodeForm.postcode, LocalDate.parse(dob))
              submitPersonalDetails(personalDetails, completionUrl)
            case _ => EitherT.rightT[Future, Result](BadRequest)
          }
        }.merge
      )
    } else {
      Future.successful(BadRequest)
    }
  }

  private def submitPersonalDetails(personalDetails: PersonalDetails, completionUrl: CompletionUrl)(implicit request: Request[_]) : EitherT[Future, Result, Result] = {
    for {
      pdv <- personalDetailsSubmission.submitPersonalDetails(personalDetails, completionUrl)
      result = pdv match {
        case FailedPersonalDetailsValidation(_) => {
          val cleanedSession = pdvSessionKeys.foldLeft(request.session)(_.-(_))
          Ok(personal_details_main(initialForm.withGlobalError("personal-details.validation.failed"), completionUrl)).withSession(cleanedSession)
        }
        case _ => personalDetailsSubmission.result(completionUrl, pdv)
      }
    } yield result
  }

  private def retrieveMainDetails(implicit request: Request[_]): (Option[String], Option[String], Option[String]) =
    (request.session.get(FIRST_NAME_KEY), request.session.get(LAST_NAME_KEY), request.session.get(DOB_KEY))

  private def hasMainDetailsAndIsMultiPage(implicit request: Request[_]): Boolean =
    retrieveMainDetails match {
      case (Some(_), Some(_), Some(_)) if appConfig.isMultiPageEnabled => true
      case _ => false
    }

  def submit(completionUrl: CompletionUrl, alternativeVersion: Boolean): Action[AnyContent] = Action.async { implicit request =>
    personalDetailsSubmission.submit(completionUrl, alternativeVersion)
  }
}