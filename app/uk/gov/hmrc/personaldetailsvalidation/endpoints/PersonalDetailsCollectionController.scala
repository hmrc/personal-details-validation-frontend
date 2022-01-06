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
import uk.gov.hmrc.personaldetailsvalidation.monitoring.{EventDispatcher, TimedOut, TimeoutContinue, UnderNinoAge}
import uk.gov.hmrc.personaldetailsvalidation.views.html.pages.we_cannot_check_your_identity
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

  private val pdvSessionKeys: List[String] = List("firstName", "lastName", "dob")

  def showPage(implicit completionUrl: CompletionUrl, origin: Option[String]): Action[AnyContent] = {
    Action.async { implicit request =>
      authorised() {
        appConfig.isLoggedInUser = Future.successful(true)
        appConfig.isLoggedInUser
      }.recover {
        case _: Exception => appConfig.isLoggedInUser = Future.successful(false)
      }.map { _ =>
        val sessionWithOrigin: Session = origin.fold[Session](request.session)(origin => request.session + ("origin" -> origin))
        Redirect(routes.PersonalDetailsCollectionController.enterYourDetails(completionUrl)).withSession(sessionWithOrigin)
      }
    }
  }

  def enterYourDetails(implicit completionUrl: CompletionUrl): Action[AnyContent] = Action.async {
    implicit request =>
      appConfig.isLoggedInUser.map { isLoggedIn =>
        Ok(enter_your_details(initialForm, completionUrl, loggedInUser = isLoggedIn))
      }
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
          Future.successful(Redirect(routes.PersonalDetailsCollectionController.whatIsYourNino(completionUrl))
              .withSession(Session(updatedSessionData))
            )
        }
      )
    }
  }

  def whatIsYourNino(completionUrl: CompletionUrl): Action[AnyContent] = Action.async { implicit request =>
    appConfig.isLoggedInUser.flatMap {
      isLoggedIn => Future.successful(Ok(what_is_your_nino(ninoForm, completionUrl, isLoggedIn)))
    }
  }

  def submitYourNino(completionUrl: CompletionUrl): Action[AnyContent] = Action.async { implicit request =>
      appConfig.isLoggedInUser.flatMap { isLoggedIn =>
        ninoForm.bindFromRequest().fold (
          formWithErrors => Future.successful(Ok(what_is_your_nino(formWithErrors, completionUrl, isLoggedIn))),
          ninoForm => {
            retrieveMainDetails match {
              case (Some(fn), Some(ln), Some(dob)) =>
                val personalDetails = PersonalDetailsWithNino(NonEmptyString(fn), NonEmptyString(ln),ninoForm.nino, LocalDate.parse(dob))
                submitPersonalDetails(personalDetails, completionUrl, isLoggedIn)
              case _ => Future.successful(BadRequest)
            }
          }
        )
      }
  }

  def whatIsYourPostCode(completionUrl: CompletionUrl): Action[AnyContent] = Action.async { implicit request =>
    appConfig.isLoggedInUser.flatMap {
      isLoggedIn => Future.successful(Ok(what_is_your_postcode(postcodeForm, completionUrl, isLoggedIn)))
    }
  }

  def submitYourPostCode(completionUrl: CompletionUrl): Action[AnyContent] = Action.async { implicit request =>
      appConfig.isLoggedInUser.flatMap { isLoggedIn =>
        postcodeForm.bindFromRequest().fold (
          formWithErrors => Future.successful(Ok(what_is_your_postcode(formWithErrors, completionUrl, isLoggedIn))),
          postCodeForm => {
            retrieveMainDetails match {
              case (Some(fn), Some(ln), Some(dob)) =>
                val personalDetails = PersonalDetailsWithPostcode(NonEmptyString(fn), NonEmptyString(ln), postCodeForm.postcode, LocalDate.parse(dob))
                submitPersonalDetails(personalDetails, completionUrl, isLoggedIn)
              case _ => Future.successful(BadRequest)
            }
          }
        )
      }
  }

  private def submitPersonalDetails(personalDetails: PersonalDetails, completionUrl: CompletionUrl, isLoggedInUser: Boolean)(implicit request: Request[_]): Future[Result] = {
    for {
      pdv <- personalDetailsSubmission.submitPersonalDetails(personalDetails)
      result = pdv match {
        case SuccessfulPersonalDetailsValidation(_) => personalDetailsSubmission.successResult(completionUrl, pdv)
        case _ =>
          val cleanedSession = pdvSessionKeys.foldLeft(request.session)(_.-(_))
          Ok(enter_your_details(initialForm.withGlobalError("personal-details.validation.failed"), completionUrl, isLoggedInUser))
            .withSession(cleanedSession)
      }
    } yield result
  }.recover {
    case _ => Redirect(completionUrl.value)
  }

  private def retrieveMainDetails(implicit request: Request[_]): (Option[String], Option[String], Option[String]) =
    (request.session.get(FIRST_NAME_KEY), request.session.get(LAST_NAME_KEY), request.session.get(DOB_KEY))


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
    eventDispatcher.dispatchEvent(UnderNinoAge)
    Future.successful(Ok(weCannotCheckYourIdentityPage()))
  }
}
