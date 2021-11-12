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

import cats.data.EitherT
import cats.implicits._
import play.api.data.Forms.mapping
import play.api.data.{Form, FormError, Mapping}
import play.api.i18n.MessagesApi
import play.api.mvc._
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.config.{AppConfig, DwpMessagesApiProvider}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.language.DwpI18nSupport
import uk.gov.hmrc.personaldetailsvalidation.connectors.IdentityVerificationConnector
import uk.gov.hmrc.personaldetailsvalidation.model._
import uk.gov.hmrc.personaldetailsvalidation.monitoring.{EventDispatcher, TimedOut, TimeoutContinue}
import uk.gov.hmrc.personaldetailsvalidation.views.html.pages.we_cannot_check_your_identity
import uk.gov.hmrc.personaldetailsvalidation.views.html.template._
import uk.gov.hmrc.personaldetailsvalidation.views.pages.PersonalDetailsPage
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.views.ViewConfig

import java.time.temporal.ChronoUnit
import java.time.{LocalDate, Period}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PersonalDetailsCollectionController @Inject()(page: PersonalDetailsPage,
                                                    personalDetailsSubmission: FuturedPersonalDetailsSubmission,
                                                    appConfig: AppConfig,
                                                    val eventDispatcher: EventDispatcher,
                                                    val controllerComponents: MessagesControllerComponents,
                                                    enterYourDetailsNino: enter_your_details_nino,
                                                    enterYourDetailsPostcode: enter_your_details_postcode,
                                                    what_is_your_postcode: what_is_your_postcode,
                                                    what_is_your_nino: what_is_your_nino,
                                                    enter_your_details: enter_your_details,
                                                    personalDetailsMain: personal_details_main,
                                                    weCannotCheckYourIdentityPage : we_cannot_check_your_identity,
                                                    ivConnector: IdentityVerificationConnector)
                                                   (implicit val authConnector: AuthConnector,
                                                    val dwpMessagesApiProvider: DwpMessagesApiProvider,
                                                    viewConfig: ViewConfig,
                                                    ec: ExecutionContext,
                                                    messagesApi: MessagesApi)
  extends DwpI18nSupport(appConfig, messagesApi) with FrontendBaseController with AuthorisedFunctions {

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

  def showPage(implicit completionUrl: CompletionUrl, alternativeVersion: Boolean, origin: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      authorised(){
        appConfig.isLoggedInUser = Future.successful(true)
        appConfig.isLoggedInUser
      }.recover {
        case ex: Exception => appConfig.isLoggedInUser = Future.successful(false)
      }.flatMap { _ =>
        appConfig.isLoggedInUser.map { isLoggedIn =>
          val sessionWithOrigin: Session = origin.fold[Session](request.session)(origin => request.session + ("origin" -> origin))
          if (appConfig.isMultiPageEnabled) {
            val form: Form[InitialPersonalDetails] = retrieveMainDetails match {
              case (Some(firstName), Some(lastName), Some(dob)) =>
                val pd = InitialPersonalDetails(NonEmptyString(firstName), NonEmptyString(lastName), LocalDate.parse(dob))
                initialForm.fill(pd)
              case _ => initialForm
            }
            Redirect(routes.PersonalDetailsCollectionController.enterYourDetails(completionUrl, origin = origin)).withSession(sessionWithOrigin)
          } else {
            Ok(page.render(alternativeVersion, isLoggedIn)).withSession(sessionWithOrigin)
          }
        }
      }
    }

  def submitMainDetails(completionUrl: CompletionUrl): Action[AnyContent] = Action.async { implicit request =>
    appConfig.isLoggedInUser.flatMap { isLoggedIn =>
      initialForm.bindFromRequest().fold (
        formWithErrors => Future.successful(Ok(personalDetailsMain(formWithErrors, completionUrl, isLoggedIn))),
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

  }

  def enterYourDetails(implicit completionUrl: CompletionUrl, alternativeVersion: Boolean, origin: Option[String]):
    Action[AnyContent] = Action { implicit request =>
       Ok(enter_your_details(initialForm, completionUrl, false))
    }


  def submitYourDetails(completionUrl: CompletionUrl): Action[AnyContent] = Action.async { implicit request =>
    appConfig.isLoggedInUser.flatMap { isLoggedIn =>
      initialForm.bindFromRequest().fold (
        formWithErrors => {
          val tooYoung: Boolean = formWithErrors.errors.contains(FormError("dateOfBirth",List("personal-details.dateOfBirth.tooYoung"),List()))
          if (tooYoung) Future.successful(Redirect(routes.PersonalDetailsCollectionController.weCannotCheckYourIdentity()))
          else Future.successful(Ok(enter_your_details(formWithErrors, completionUrl, isLoggedIn)))
        },
        mainDetails => {
          val updatedSessionData = request.session.data ++ Map(
            FIRST_NAME_KEY -> mainDetails.firstName.value,
            LAST_NAME_KEY -> mainDetails.lastName.value,
            DOB_KEY -> mainDetails.dateOfBirth.toString
          )
          if (appConfig.isMultiPageEnabled){
            Future.successful(Redirect(routes.PersonalDetailsCollectionController.whatIsYourNino(completionUrl))
              .withSession(Session(updatedSessionData))
            )
          } else {
            Future.successful(Redirect(routes.PersonalDetailsCollectionController.showNinoForm(completionUrl))
              .withSession(Session(updatedSessionData))
            )
          }
        }
      )
    }

  }

  def showNinoForm(completionUrl: CompletionUrl) = Action.async { implicit request =>
    if (hasMainDetailsAndIsMultiPage) {
      appConfig.isLoggedInUser.flatMap {
        isLoggedIn => Future.successful(Ok(enterYourDetailsNino(ninoForm, completionUrl, isLoggedIn)))
      }
    } else
      Future.successful(Redirect(routes.PersonalDetailsCollectionController.showPage(completionUrl, false, None)))
  }

  def whatIsYourNino(completionUrl: CompletionUrl) = Action.async { implicit request =>
    appConfig.isLoggedInUser.flatMap {
      isLoggedIn => Future.successful(Ok(what_is_your_nino(ninoForm, completionUrl, isLoggedIn)))
    }
  }

  def submitYourNino(completionUrl: CompletionUrl) = Action.async { implicit request =>
    if (appConfig.isMultiPageEnabled) {
      appConfig.isLoggedInUser.flatMap { isLoggedIn =>
        ninoForm.bindFromRequest().fold (
          formWithErrors => Future.successful(Ok(what_is_your_nino(formWithErrors, completionUrl, isLoggedIn))),
          ninoForm => {
            retrieveMainDetails match {
              case (Some(fn), Some(ln), Some(dob)) =>
                val personalDetails = PersonalDetailsWithNino(NonEmptyString(fn), NonEmptyString(ln),ninoForm.nino, LocalDate.parse(dob))
                submitPersonalDetails(personalDetails, completionUrl, isLoggedIn)
              case _ => EitherT.rightT[Future, Result](BadRequest)
            }
          }.merge
        )
      }
    } else {
      Future.successful(BadRequest)
    }
  }

  def submitNino(completionUrl: CompletionUrl) = Action.async { implicit request =>
    if (appConfig.isMultiPageEnabled) {
      appConfig.isLoggedInUser.flatMap { isLoggedIn =>
        ninoForm.bindFromRequest().fold(
          formWithErrors => Future.successful(Ok(enterYourDetailsNino(formWithErrors, completionUrl, isLoggedIn))),
          ninoForm => {
            retrieveMainDetails match {
              case (Some(fn), Some(ln), Some(dob)) =>
                val personalDetails = PersonalDetailsWithNino(NonEmptyString(fn), NonEmptyString(ln), ninoForm.nino, LocalDate.parse(dob))
                submitPersonalDetails(personalDetails, completionUrl, isLoggedIn)
              case _ => EitherT.rightT[Future, Result](BadRequest)
            }
          }.merge
        )
      }

    } else {
      Future.successful(BadRequest)
    }
  }

  @Deprecated // will be removed after whatIsYourPostCode goes live
  def showPostCodeForm(completionUrl: CompletionUrl) = Action.async { implicit request =>
    if (hasMainDetailsAndIsMultiPage) {
      appConfig.isLoggedInUser.flatMap {
        isLoggedIn => Future.successful(Ok(enterYourDetailsPostcode(postcodeForm, completionUrl, isLoggedIn)))
      }
    } else
      Future.successful(Redirect(routes.PersonalDetailsCollectionController.showPage(completionUrl, false, None)))
  }

  /** This endpoint will replace showPostCodeForm after it goes live  */
  def whatIsYourPostCode(completionUrl: CompletionUrl) = Action.async { implicit request =>
    appConfig.isLoggedInUser.flatMap {
      isLoggedIn => Future.successful(Ok(what_is_your_postcode(postcodeForm, completionUrl, isLoggedIn)))
    }
  }

  def submitYourPostCode(completionUrl: CompletionUrl) = Action.async { implicit request =>
    if (appConfig.isMultiPageEnabled) {
      appConfig.isLoggedInUser.flatMap { isLoggedIn =>
        postcodeForm.bindFromRequest().fold (
          formWithErrors => Future.successful(Ok(what_is_your_postcode(formWithErrors, completionUrl, isLoggedIn))),
          postCodeForm => {
            retrieveMainDetails match {
              case (Some(fn), Some(ln), Some(dob)) =>
                val personalDetails = PersonalDetailsWithPostcode(NonEmptyString(fn), NonEmptyString(ln),postCodeForm.postcode, LocalDate.parse(dob))
                submitPersonalDetails(personalDetails, completionUrl, isLoggedIn)
              case _ => EitherT.rightT[Future, Result](BadRequest)
            }
          }.merge
        )
      }
    } else {
      Future.successful(BadRequest)
    }
  }

  def submitPostcode(completionUrl: CompletionUrl) = Action.async { implicit request =>
    if (appConfig.isMultiPageEnabled) {
      appConfig.isLoggedInUser.flatMap { isLoggedIn =>
        postcodeForm.bindFromRequest().fold (
          formWithErrors => Future.successful(Ok(enterYourDetailsPostcode(formWithErrors, completionUrl, isLoggedIn))),
          postCodeForm => {
            retrieveMainDetails match {
              case (Some(fn), Some(ln), Some(dob)) =>
                val personalDetails = PersonalDetailsWithPostcode(NonEmptyString(fn), NonEmptyString(ln),postCodeForm.postcode, LocalDate.parse(dob))
                submitPersonalDetails(personalDetails, completionUrl, isLoggedIn)
              case _ => EitherT.rightT[Future, Result](BadRequest)
            }
          }.merge
        )
      }
    } else {
      Future.successful(BadRequest)
    }
  }

  private def submitPersonalDetails(personalDetails: PersonalDetails, completionUrl: CompletionUrl, isLoggedInUser: Boolean)(implicit request: Request[_]) : EitherT[Future, Result, Result] = {
    for {
      pdv <- personalDetailsSubmission.submitPersonalDetails(personalDetails, completionUrl, isLoggedInUser = isLoggedInUser)
      result = pdv match {
        case FailedPersonalDetailsValidation(_) => {
          val cleanedSession = pdvSessionKeys.foldLeft(request.session)(_.-(_))
          Ok(personalDetailsMain(initialForm.withGlobalError("personal-details.validation.failed"), completionUrl, isLoggedInUser)).withSession(cleanedSession)
        }
        case _ => personalDetailsSubmission.result(completionUrl, pdv, isLoggedInUser = isLoggedInUser)
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
    appConfig.isLoggedInUser.flatMap {isLoggedInUser =>
      personalDetailsSubmission.submit(completionUrl, alternativeVersion, isLoggedInUser)
    }

  }

  /**
    * redirect user to the completionUrl with a timeout status
    */
  def redirectAfterTimeout(completionUrl: CompletionUrl): Action[AnyContent] = Action.async { implicit request =>
    eventDispatcher.dispatchEvent(TimedOut)
    ivConnector.updateJourney(completionUrl.value)
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

  def weCannotCheckYourIdentity(): Action[AnyContent] = Action.async { implicit request =>
      Future.successful(Ok(weCannotCheckYourIdentityPage()))
  }
}
