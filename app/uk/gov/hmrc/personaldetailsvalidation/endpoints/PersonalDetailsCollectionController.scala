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

package uk.gov.hmrc.personaldetailsvalidation.endpoints

import cats.implicits._
import javax.inject.Inject
import play.api.data.{Form, Mapping}
import play.api.data.Forms.mapping
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.config.AppConfig
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.personaldetailsvalidation.model._
import uk.gov.hmrc.personaldetailsvalidation.views.pages.PersonalDetailsPage
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.personaldetailsvalidation.views.html.template._
import uk.gov.hmrc.views.ViewConfig
import java.time.LocalDate
import cats.data.EitherT
import scala.concurrent.Future
import scala.util.Try

class PersonalDetailsCollectionController @Inject()(page: PersonalDetailsPage,
                                                    personalDetailsSubmission: FuturedPersonalDetailsSubmission,
                                                    appConfig: AppConfig)(implicit val messagesApi: MessagesApi, viewConfig: ViewConfig)
  extends FrontendController with I18nSupport {

  import uk.gov.hmrc.formmappings.Mappings._

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

  private lazy val ninoForm: Form[NinoDetails] = Form(mapping(
    "nino" -> ninoValidation()
  )(NinoDetails.apply)(NinoDetails.unapply))

  private lazy val postcodeForm: Form[PostcodeDetails] = Form(mapping(
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
      val form: Form[InitialPersonalDetails] = (request.session.get("firstName"), request.session.get("lastName"), request.session.get("dob")) match {
        case (Some(fn), Some(ln), Some(dob)) =>
          val pd = InitialPersonalDetails(NonEmptyString(fn), NonEmptyString(ln), LocalDate.parse(dob))
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
          "firstName" -> mainDetails.firstName.value,
          "lastName" -> mainDetails.lastName.value,
          "dob" -> mainDetails.dateOfBirth.toString
        )
        Future.successful(Redirect(routes.PersonalDetailsCollectionController.showNinoForm(completionUrl))
          .withSession(Session(updatedSessionData))
        )
      }
    )
  }

  def showNinoForm(completionUrl: CompletionUrl) = Action.async { implicit request =>
    (request.session.get("firstName"), request.session.get("lastName"), request.session.get("dob")) match {
      case (Some(firstName), Some(lastName), Some(dob)) if appConfig.isMultiPageEnabled => Future.successful(Ok(enter_your_details_nino(ninoForm, completionUrl)))
      case _ => Future.successful(Redirect(routes.PersonalDetailsCollectionController.showPage(completionUrl, false)))
    }
  }

  def submitNino(completionUrl: CompletionUrl) = Action.async { implicit request =>
    if (appConfig.isMultiPageEnabled) {
      ninoForm.bindFromRequest().fold(
        formWithErrors => Future.successful(Ok(enter_your_details_nino(formWithErrors, completionUrl))),
        ninoForm => {
          val personalDetails: PersonalDetails = (request.session.get("firstName"), request.session.get("lastName"), request.session.get("dob")) match {
            case (Some(fn), Some(ln), Some(dob)) => PersonalDetailsWithNino(NonEmptyString(fn), NonEmptyString(ln), ninoForm.nino, LocalDate.parse(dob))
          }
          submitPersonalDetails(personalDetails, completionUrl)
        }.merge
      )
    } else {
      Future.successful(BadRequest)
    }
  }

  def showPostCodeForm(completionUrl: CompletionUrl) = Action.async { implicit request =>
    (request.session.get("firstName"), request.session.get("lastName"), request.session.get("dob")) match {
      case (Some(firstName), Some(lastName), Some(dob)) if appConfig.isMultiPageEnabled => Future.successful(Ok(enter_your_details_postcode(postcodeForm, completionUrl)))
      case _ => Future.successful(Redirect(routes.PersonalDetailsCollectionController.showPage(completionUrl, false)))
    }
  }

  def submitPostcode(completionUrl: CompletionUrl) = Action.async { implicit request =>
    if (appConfig.isMultiPageEnabled) {
      postcodeForm.bindFromRequest().fold (
        formWithErrors => Future.successful(Ok(enter_your_details_postcode(formWithErrors, completionUrl))),
        postCodeForm => {
          val personalDetails : PersonalDetails = (request.session.get("firstName"), request.session.get("lastName"), request.session.get("dob")) match {
            case (Some(fn), Some(ln), Some(dob)) => PersonalDetailsWithPostcode(NonEmptyString(fn), NonEmptyString(ln),postCodeForm.postcode, LocalDate.parse(dob))
          }
          submitPersonalDetails(personalDetails, completionUrl)
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

  def submit(completionUrl: CompletionUrl, alternativeVersion: Boolean): Action[AnyContent] = Action.async { implicit request =>
    personalDetailsSubmission.submit(completionUrl, alternativeVersion)
  }
}