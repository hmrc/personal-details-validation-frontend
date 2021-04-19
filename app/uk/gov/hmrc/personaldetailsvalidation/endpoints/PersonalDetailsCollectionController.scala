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

package uk.gov.hmrc.personaldetailsvalidation.endpoints

import java.time.LocalDate

import cats.data.EitherT
import cats.implicits._
import javax.inject.Inject
import play.api.data.Forms.mapping
import play.api.data.{Form, Mapping}
import play.api.i18n.MessagesApi
import play.api.mvc._
import uk.gov.hmrc.config.{AppConfig, DwpMessagesApiProvider}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.language.DwpI18nSupport
import uk.gov.hmrc.personaldetailsvalidation.model._
import uk.gov.hmrc.personaldetailsvalidation.monitoring.{EventDispatcher, TimedOut, TimeoutContinue}
import uk.gov.hmrc.personaldetailsvalidation.views.html.template.{enter_your_details_nino, enter_your_details_postcode, personal_details_main}
import uk.gov.hmrc.personaldetailsvalidation.views.pages.PersonalDetailsPage
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.views.ViewConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PersonalDetailsCollectionController @Inject()(page: PersonalDetailsPage,
                                                    personalDetailsSubmission: FuturedPersonalDetailsSubmission,
                                                    appConfig: AppConfig,
                                                    val eventDispatcher: EventDispatcher,
                                                    val controllerComponents: MessagesControllerComponents,
                                                    enterYourDetailsNino: enter_your_details_nino,
                                                    enterYourDetailsPostcode: enter_your_details_postcode,
                                                    personalDetailsMain: personal_details_main)
                                                   (implicit val dwpMessagesApiProvider: DwpMessagesApiProvider,
                                                    viewConfig: ViewConfig,
                                                    ec: ExecutionContext,
                                                    messagesApi: MessagesApi)
  extends DwpI18nSupport(appConfig, messagesApi) with FrontendBaseController {

//  override implicit lazy val messagesApi: MessagesApi = controllerComponents.messagesApi

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
      Ok(personalDetailsMain(form, completionUrl))
    } else {
      Ok(page.render(alternativeVersion))
    }
  }

  def submitMainDetails(completionUrl: CompletionUrl): Action[AnyContent] = Action.async { implicit request =>
    initialForm.bindFromRequest().fold (
      formWithErrors => Future.successful(Ok(personalDetailsMain(formWithErrors, completionUrl))),
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
      Future.successful(Ok(enterYourDetailsNino(ninoForm, completionUrl)))
    else
      Future.successful(Redirect(routes.PersonalDetailsCollectionController.showPage(completionUrl, false)))
  }

  def submitNino(completionUrl: CompletionUrl) = Action.async { implicit request =>
    if (appConfig.isMultiPageEnabled) {
      ninoForm.bindFromRequest().fold(
        formWithErrors => Future.successful(Ok(enterYourDetailsNino(formWithErrors, completionUrl))),
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
      Future.successful(Ok(enterYourDetailsPostcode(postcodeForm, completionUrl)))
    else
      Future.successful(Redirect(routes.PersonalDetailsCollectionController.showPage(completionUrl, false)))
  }

  def submitPostcode(completionUrl: CompletionUrl) = Action.async { implicit request =>
    if (appConfig.isMultiPageEnabled) {
      postcodeForm.bindFromRequest().fold (
        formWithErrors => Future.successful(Ok(enterYourDetailsPostcode(formWithErrors, completionUrl))),
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
          Ok(personalDetailsMain(initialForm.withGlobalError("personal-details.validation.failed"), completionUrl)).withSession(cleanedSession)
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

  /**
    * redirect user to the completionUrl with a timeout status
    */
  def redirectAfterTimeout(completionUrl: CompletionUrl): Action[AnyContent] = Action.async { implicit request =>
    eventDispatcher.dispatchEvent(TimedOut)
    Future.successful(Redirect(completionUrl.value, Map("userTimeout" -> Seq(""))))
  }

  /**
    * Endpoint which just has the side-effect of extending the Play session to avoid (the 15 min) timeout
    *
    * */
  def keepAlive: Action[AnyContent] = Action.async { implicit request =>
    eventDispatcher.dispatchEvent(TimeoutContinue)
    Future.successful(Ok("OK"))
  }


}