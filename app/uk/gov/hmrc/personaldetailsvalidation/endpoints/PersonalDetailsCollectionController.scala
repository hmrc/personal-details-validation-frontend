/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.data.FormError
import play.api.i18n.MessagesApi
import play.api.mvc._
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.config.{AppConfig, DwpMessagesApiProvider}
import uk.gov.hmrc.language.DwpI18nSupport
import uk.gov.hmrc.personaldetailsvalidation.connectors.IdentityVerificationConnector
import uk.gov.hmrc.personaldetailsvalidation.model.InitialPersonalDetailsForm.initialForm
import uk.gov.hmrc.personaldetailsvalidation.model.NinoDetailsForm.ninoForm
import uk.gov.hmrc.personaldetailsvalidation.model.PostcodeDetailsForm.postcodeForm
import uk.gov.hmrc.personaldetailsvalidation.model._
import uk.gov.hmrc.personaldetailsvalidation.monitoring.{EventDispatcher, PdvFailedAttempt, PdvLockedOut, TimedOut, TimeoutContinue, UnderNinoAge}
import uk.gov.hmrc.personaldetailsvalidation.views.html.pages._
import uk.gov.hmrc.personaldetailsvalidation.views.html.template._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.views.ViewConfig
import java.time.LocalDate

import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}

class PersonalDetailsCollectionController @Inject()(personalDetailsSubmission: PersonalDetailsSubmission,
                                                    appConfig: AppConfig,
                                                    val eventDispatcher: EventDispatcher,
                                                    val controllerComponents: MessagesControllerComponents,
                                                    what_is_your_postcode: what_is_your_postcode,
                                                    what_is_your_nino: what_is_your_nino,
                                                    enter_your_details: enter_your_details,
                                                    incorrect_details: incorrect_details,
                                                    locked_out: locked_out,
                                                    weCannotCheckYourIdentityPage : we_cannot_check_your_identity,
                                                    ivConnector: IdentityVerificationConnector)
                                                   (implicit val authConnector: AuthConnector,
                                                    val dwpMessagesApiProvider: DwpMessagesApiProvider,
                                                    viewConfig: ViewConfig,
                                                    ec: ExecutionContext,
                                                    messagesApi: MessagesApi)
  extends DwpI18nSupport(appConfig, messagesApi) with FrontendBaseController with AuthorisedFunctions {

  private final val FIRST_NAME_KEY = "firstName"
  private final val LAST_NAME_KEY = "lastName"
  private final val DOB_KEY = "dob"

  private val pdvSessionKeys : List[String] = List("firstName", "lastName", "dob")

  def showPage(implicit completionUrl: CompletionUrl, origin: Option[String], failureUrl: Option[CompletionUrl]): Action[AnyContent] =
    Action.async { implicit request =>
      val sessionWithOrigin: Session = origin.fold[Session](request.session)(origin => request.session + ("origin" -> origin))
      personalDetailsSubmission.getUserAttempts().map { attempts =>
        if (appConfig.retryIsEnabled) {
          if (attempts < appConfig.retryLimit) {
            Redirect(routes.PersonalDetailsCollectionController.enterYourDetails(completionUrl, false, failureUrl)).withSession(sessionWithOrigin)
          } else if (failureUrl.isDefined) {
              Redirect(failureUrl.get.value)
            } else {
              Redirect(routes.PersonalDetailsCollectionController.lockedOut())
            }
        } else {
          Redirect(routes.PersonalDetailsCollectionController.enterYourDetails(completionUrl, false, failureUrl)).withSession(sessionWithOrigin)
        }
      }
    }

  def enterYourDetails(completionUrl: CompletionUrl, withError: Boolean = false, failureUrl: Option[CompletionUrl]): Action[AnyContent] = Action.async { implicit request =>
    viewConfig.isLoggedIn.map { isLoggedIn: Boolean =>
      if (withError) {
        Ok(enter_your_details(initialForm.withGlobalError("personal-details.validation.failed"), completionUrl, loggedInUser = isLoggedIn, failureUrl))
      } else {
        Ok(enter_your_details(initialForm, completionUrl, loggedInUser = isLoggedIn, failureUrl))
      }
    }
  }


  def submitYourDetails(completionUrl: CompletionUrl, failureUrl: Option[CompletionUrl]): Action[AnyContent] = Action.async { implicit request =>
    viewConfig.isLoggedIn.flatMap { isLoggedIn: Boolean =>
      initialForm.bindFromRequest().fold (
        formWithErrors => {
          val tooYoung: Boolean = formWithErrors.errors.contains(FormError("dateOfBirth",List("personal-details.dateOfBirth.tooYoung"),List()))
          if (tooYoung) Future.successful(Redirect(routes.PersonalDetailsCollectionController.weCannotCheckYourIdentity()))
          else Future.successful(Ok(enter_your_details(formWithErrors, completionUrl, isLoggedIn, failureUrl)))
        },
        mainDetails => {
          val updatedSessionData = request.session.data ++ Map(
            FIRST_NAME_KEY -> mainDetails.firstName.value,
            LAST_NAME_KEY -> mainDetails.lastName.value,
            DOB_KEY -> mainDetails.dateOfBirth.toString
          )
          Future.successful(
            Redirect(routes.PersonalDetailsCollectionController.whatIsYourNino(completionUrl, failureUrl)).withSession(Session(updatedSessionData))
          )
        }
      )
    }
  }

  def whatIsYourNino(completionUrl: CompletionUrl, failureUrl: Option[CompletionUrl]): Action[AnyContent] = Action.async { implicit request =>
    viewConfig.isLoggedIn.flatMap { isLoggedIn: Boolean =>
      Future.successful(Ok(what_is_your_nino(ninoForm, completionUrl, isLoggedIn, failureUrl)))
    }
  }

  def submitYourNino(completionUrl: CompletionUrl, failureUrl: Option[CompletionUrl]): Action[AnyContent] = Action.async { implicit request =>
    viewConfig.isLoggedIn.flatMap { isLoggedIn: Boolean =>
      ninoForm.bindFromRequest().fold (
        formWithErrors => Future.successful(Ok(what_is_your_nino(formWithErrors, completionUrl, isLoggedIn, failureUrl))),
        ninoForm => {
          retrieveMainDetails match {
            case (Some(fn), Some(ln), Some(dob)) =>
              val personalDetails = PersonalDetailsWithNino(NonEmptyString(fn), NonEmptyString(ln),ninoForm.nino, LocalDate.parse(dob))
              submitPersonalDetails(personalDetails, completionUrl, failureUrl)
            case _ => Future.successful(BadRequest)
          }
        }
      )
    }
  }

  def whatIsYourPostCode(completionUrl: CompletionUrl, failureUrl: Option[CompletionUrl]): Action[AnyContent] = Action.async { implicit request =>
    viewConfig.isLoggedIn.flatMap { isLoggedIn: Boolean =>
       Future.successful(Ok(what_is_your_postcode(postcodeForm, completionUrl, isLoggedIn, failureUrl)))
    }
  }

  def submitYourPostCode(completionUrl: CompletionUrl, failureUrl: Option[CompletionUrl]): Action[AnyContent] = Action.async { implicit request =>
    viewConfig.isLoggedIn.flatMap { isLoggedIn: Boolean =>
      postcodeForm.bindFromRequest().fold (
        formWithErrors => Future.successful(Ok(what_is_your_postcode(formWithErrors, completionUrl, isLoggedIn, failureUrl))),
        postCodeForm => {
          retrieveMainDetails match {
            case (Some(fn), Some(ln), Some(dob)) =>
              val personalDetails = PersonalDetailsWithPostcode(NonEmptyString(fn), NonEmptyString(ln), postCodeForm.postcode, LocalDate.parse(dob))
              submitPersonalDetails(personalDetails, completionUrl, failureUrl)
            case _ => Future.successful(BadRequest)
          }
        }
      )
    }
  }

  private def submitPersonalDetails(personalDetails: PersonalDetails, completionUrl: CompletionUrl, failureUrl: Option[CompletionUrl])(implicit request: Request[_]): Future[Result] = {
    for {
      pdv <- personalDetailsSubmission.submitPersonalDetails(personalDetails)
      result = pdv match {
        case SuccessfulPersonalDetailsValidation(_) => personalDetailsSubmission.successResult(completionUrl, pdv)
        case FailedPersonalDetailsValidation(_, maybeCredId, attempt) =>
          if (appConfig.retryIsEnabled && maybeCredId.nonEmpty) {
            val cleanedSession = pdvSessionKeys.foldLeft(request.session)(_.-(_))
            val attemptsRemaining = viewConfig.retryLimit - attempt
            if (attempt < appConfig.retryLimit) {
              val origin = request.session.get("origin").getOrElse("")
              val isSA = origin == "bta-sa" || origin == "pta-sa" || origin == "ssttp-sa"
              eventDispatcher.dispatchEvent(PdvFailedAttempt(appConfig.retryLimit - attempt))
              if (isSA) Redirect(routes.PersonalDetailsCollectionController.incorrectDetailsForSa(completionUrl, attemptsRemaining, failureUrl)).withSession(cleanedSession)
              else Redirect(routes.PersonalDetailsCollectionController.incorrectDetails(completionUrl, attemptsRemaining, failureUrl)).withSession(cleanedSession)
            } else {
              eventDispatcher.dispatchEvent(PdvLockedOut())
              if (failureUrl.isDefined) {
                Redirect(failureUrl.get.value).withSession(cleanedSession)
              } else {
                Redirect(routes.PersonalDetailsCollectionController.lockedOut()).withSession(cleanedSession)
              }
            }
          } else {
            val cleanedSession = pdvSessionKeys.foldLeft(request.session)(_.-(_))
            Redirect(routes.PersonalDetailsCollectionController.enterYourDetails(completionUrl, withError = true, failureUrl)).withSession(cleanedSession)
          }
      }
    } yield result
  }.recover {
    case _ =>
      val redirectUrl: String = if (failureUrl.isDefined) {failureUrl.get.value} else {completionUrl.value}
      Redirect(redirectUrl)
  }

  private def retrieveMainDetails(implicit request: Request[_]): (Option[String], Option[String], Option[String]) =
    (request.session.get(FIRST_NAME_KEY), request.session.get(LAST_NAME_KEY), request.session.get(DOB_KEY))

  /**
    * redirect user to the completionUrl with a timeout status
    */
  def redirectAfterTimeout(completionUrl: CompletionUrl, failureUrl: Option[CompletionUrl]): Action[AnyContent] = Action.async { implicit request =>
    eventDispatcher.dispatchEvent(TimedOut())
    ivConnector.updateJourney(completionUrl.value)
    val redirectUrl: String = if (failureUrl.isDefined) {failureUrl.get.value} else {completionUrl.value}
    Future.successful(Redirect(redirectUrl, Map("userTimeout" -> Seq(""))))
  }

  /**
    * Endpoint which just has the side-effect of extending the Play session to avoid (the 15 min) timeout
    *
    * */
  def keepAlive: Action[AnyContent] = Action.async { implicit request =>
    eventDispatcher.dispatchEvent(TimeoutContinue())
    Future.successful(Ok("OK"))
  }

  def weCannotCheckYourIdentity(): Action[AnyContent] = Action.async { implicit request =>
    eventDispatcher.dispatchEvent(UnderNinoAge())
    Future.successful(Ok(weCannotCheckYourIdentityPage()))
  }

  def incorrectDetails(completionUrl: CompletionUrl, attemptsRemaining: Int, failureUrl: Option[CompletionUrl]): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(incorrect_details(completionUrl, attemptsRemaining, isSA = false, failureUrl)))
  }

  def incorrectDetailsForSa(completionUrl: CompletionUrl, attemptsRemaining: Int, failureUrl: Option[CompletionUrl]): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(incorrect_details(completionUrl, attemptsRemaining, isSA = true, failureUrl)))
  }

  def lockedOut(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(locked_out()))
  }
}
