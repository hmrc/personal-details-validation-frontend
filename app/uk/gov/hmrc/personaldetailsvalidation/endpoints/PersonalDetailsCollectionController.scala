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

package uk.gov.hmrc.personaldetailsvalidation.endpoints

import play.api.Logging
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
import uk.gov.hmrc.personaldetailsvalidation.model.DoYouHaveYourNino
import uk.gov.hmrc.personaldetailsvalidation.model._
import uk.gov.hmrc.personaldetailsvalidation.monitoring._
import uk.gov.hmrc.personaldetailsvalidation.monitoring.dataStreamAudit.DataStreamAuditService
import uk.gov.hmrc.personaldetailsvalidation.views.html.pages._
import uk.gov.hmrc.personaldetailsvalidation.views.html.template._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.views.ViewConfig

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PersonalDetailsCollectionController @Inject()(personalDetailsSubmission: PersonalDetailsSubmission,
                                                    appConfig: AppConfig,
                                                    dataStreamAuditService: DataStreamAuditService,
                                                    val controllerComponents: MessagesControllerComponents,
                                                    what_is_your_postcode: what_is_your_postcode,
                                                    what_is_your_nino: what_is_your_nino,
                                                    enter_your_details: enter_your_details,
                                                    do_you_have_your_nino: do_you_have_your_nino,
                                                    incorrect_details: incorrect_details,
                                                    locked_out: locked_out,
                                                    weCannotCheckYourIdentityPage : we_cannot_check_your_identity,
                                                    service_temporarily_unavailable: service_temporarily_unavailable,
                                                    you_have_been_timed_out: you_have_been_timed_out,
                                                    you_have_been_timed_out_dwp: you_have_been_timed_out_dwp,
                                                    ivConnector: IdentityVerificationConnector)
                                                   (implicit val authConnector: AuthConnector,
                                                    val dwpMessagesApiProvider: DwpMessagesApiProvider,
                                                    viewConfig: ViewConfig,
                                                    val ec: ExecutionContext,
                                                    messagesApi: MessagesApi)
  extends DwpI18nSupport(appConfig, messagesApi) with FrontendBaseController with AuthorisedFunctions with Logging {

  private final val FIRST_NAME_KEY = "firstName"
  private final val LAST_NAME_KEY = "lastName"
  private final val DOB_KEY = "dob"
  private final val HAS_NINO_KEY = "hasNino"

  private val pdvSessionKeys : List[String] = List("firstName", "lastName", "dob")

  def showPage(implicit completionUrl: CompletionUrl, origin: Option[String], failureUrl: Option[CompletionUrl]): Action[AnyContent] =
    Action.async { implicit request =>
      val sessionWithOriginAndFailureUrl: Session = origin.fold[Session](request.session)(origin => request.session + ("origin" -> origin)
        + ("failureUrl" -> failureUrl.getOrElse(CompletionUrl("")).value))
        personalDetailsSubmission.getUserAttempts().map { attemptsDetails =>
          if (attemptsDetails.attempts >= appConfig.retryLimit) {
            val pdvLockedOut = PdvLockedOut("reattempt PDV within 24 hours", attemptsDetails.maybeCredId.getOrElse(""), origin.getOrElse(""))
            dataStreamAuditService.audit(pdvLockedOut)
          }
          if (attemptsDetails.attempts < appConfig.retryLimit) {
            Redirect(routes.PersonalDetailsCollectionController.enterYourDetails(completionUrl, withError = false, failureUrl)).withSession(sessionWithOriginAndFailureUrl)
          } else if (failureUrl.isDefined) {
              Redirect(failureUrl.get.value)
            } else {
              Redirect(routes.PersonalDetailsCollectionController.lockedOut())
            }
        }
    }

  def enterYourDetails(completionUrl: CompletionUrl, withError: Boolean = false, failureUrl: Option[CompletionUrl], maybeRetryGuidanceText: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
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
            Redirect(routes.PersonalDetailsCollectionController.showHaveYourNationalInsuranceNumber(completionUrl, failureUrl)).withSession(Session(updatedSessionData))
          )
        }
      )
    }
  }

  def whatIsYourNino(completionUrl: CompletionUrl, failureUrl: Option[CompletionUrl]): Action[AnyContent] = Action.async { implicit request =>
    viewConfig.isLoggedIn.flatMap { isLoggedIn: Boolean =>
      retrieveNamesAndDOB(completionUrl, failureUrl) match {
        case Right(_) => Future.successful(Ok(what_is_your_nino(ninoForm, completionUrl, isLoggedIn, failureUrl)))
        case Left(redirect) => Future(redirect)
      }
    }
  }

  def submitYourNino(completionUrl: CompletionUrl, failureUrl: Option[CompletionUrl]): Action[AnyContent] = Action.async { implicit request =>
    viewConfig.isLoggedIn.flatMap { isLoggedIn: Boolean =>
      ninoForm.bindFromRequest().fold (
        formWithErrors => {
          logger.info("SUBMIT_NINO user received a form error when submitting")
          Future.successful(Ok(what_is_your_nino(formWithErrors, completionUrl, isLoggedIn, failureUrl)))
        },
        ninoForm => {
          retrieveNamesAndDOB(completionUrl: CompletionUrl, failureUrl: Option[CompletionUrl]) match {
            case Right((fn,ln,dob)) =>
              val personalDetails = PersonalDetailsWithNino(NonEmptyString(fn), NonEmptyString(ln),ninoForm.nino, LocalDate.parse(dob))
              submitPersonalDetails(personalDetails, completionUrl, failureUrl)
            case Left(redirect) => Future(redirect)
          }
        }
      )
    }
  }

  def whatIsYourPostCode(completionUrl: CompletionUrl, failureUrl: Option[CompletionUrl]): Action[AnyContent] = Action.async { implicit request =>
    viewConfig.isLoggedIn.flatMap { isLoggedIn: Boolean =>
      retrieveNamesAndDOB(completionUrl, failureUrl) match {
        case Right(_) => Future.successful(Ok(what_is_your_postcode(postcodeForm, completionUrl, isLoggedIn, failureUrl)))
        case Left(redirect) => Future(redirect)
      }
    }
  }

  def submitYourPostCode(completionUrl: CompletionUrl, failureUrl: Option[CompletionUrl]): Action[AnyContent] = Action.async { implicit request =>
    viewConfig.isLoggedIn.flatMap { isLoggedIn: Boolean =>
      postcodeForm.bindFromRequest().fold (
        formWithErrors => {
          logger.info("SUBMIT_POSTCODE user received a form error when submitting")
          Future.successful(Ok(what_is_your_postcode(formWithErrors, completionUrl, isLoggedIn, failureUrl)))
        },
        postCodeForm => {
          retrieveNamesAndDOB(completionUrl: CompletionUrl, failureUrl: Option[CompletionUrl]) match {
            case Right((fn, ln, dob)) =>
              val personalDetails = PersonalDetailsWithPostcode(NonEmptyString(fn), NonEmptyString(ln), postCodeForm.postcode, LocalDate.parse(dob))
              submitPersonalDetails(personalDetails, completionUrl, failureUrl)
            case Left(redirect) => Future(redirect)
          }
        }
      )
    }
  }

  private def retrieveNamesAndDOB(completionUrl: CompletionUrl, failureUrl: Option[CompletionUrl])(implicit request: MessagesRequest[AnyContent]): Either[Result, (String, String, String)] = {
    (request.session.get(FIRST_NAME_KEY), request.session.get(LAST_NAME_KEY), request.session.get(DOB_KEY)) match {
      case (Some(fn), Some(ln), Some(dob)) => Right((fn, ln, dob))
      case (fn, ln, dob) =>
        logger.warn("Missing values in session:" +
          s"${if(fn.isEmpty){" firstName"}else{""}}" +
          s"${if(ln.isEmpty){" lastName"}else{""}}" +
          s"${if(dob.isEmpty){" dateOfBirth"}else{""}}")
        Left(Redirect(routes.PersonalDetailsCollectionController.enterYourDetails(completionUrl, withError = false, failureUrl)))
    }
  }

  def showHaveYourNationalInsuranceNumber(completionUrl: CompletionUrl, failureUrl: Option[CompletionUrl]): Action[AnyContent] = Action.async { implicit request =>
    viewConfig.isLoggedIn.flatMap { isLoggedIn: Boolean =>
      Future.successful(Ok(do_you_have_your_nino(DoYouHaveYourNino.apply(), completionUrl, isLoggedIn, failureUrl)))
    }
  }

  def processHaveYourNationalInsuranceNumber(completionUrl: CompletionUrl, failureUrl: Option[CompletionUrl]): Action[AnyContent] = Action.async { implicit request =>
    viewConfig.isLoggedIn.flatMap { isLoggedIn: Boolean =>
      DoYouHaveYourNino.apply().bindFromRequest().fold(
        errors =>
          Future.successful(BadRequest(do_you_have_your_nino(
            errors, completionUrl, isLoggedIn, failureUrl))),
        {
          case UserHasNinoTrue => Future.successful(Redirect(routes.PersonalDetailsCollectionController.whatIsYourNino(completionUrl, failureUrl))
            .withSession(Session(request.session.data ++ Map(HAS_NINO_KEY -> "yes"))))
          case UserHasNinoFalse => Future.successful(Redirect(routes.PersonalDetailsCollectionController.whatIsYourPostCode(completionUrl, failureUrl))
            .withSession(Session(request.session.data ++ Map(HAS_NINO_KEY -> "no"))))
        }
      )
    }
  }

  private def submitPersonalDetails(personalDetails: PersonalDetails, completionUrl: CompletionUrl, failureUrl: Option[CompletionUrl])(implicit request: Request[_]): Future[Result] = {
    for {
      pdv <- personalDetailsSubmission.submitPersonalDetails(personalDetails)
      result = pdv match {
        case SuccessfulPersonalDetailsValidation(_, deceased) =>
          if (deceased) {
            val cleanedSession = pdvSessionKeys.foldLeft(request.session)(_.-(_))
            logger.info(s"Session keys wiped for ${hc.sessionId}")
            ivConnector.updateJourney(completionUrl.value, "Deceased")
            Redirect(routes.PersonalDetailsCollectionController.redirectToHelplineServiceDeceasedPage()).withSession(cleanedSession)
          } else {
            personalDetailsSubmission.successResult(completionUrl, pdv)
          }
        case FailedPersonalDetailsValidation(_, maybeCredId, attempt) =>
          if (maybeCredId.nonEmpty) {
            val cleanedSession = pdvSessionKeys.foldLeft(request.session)(_.-(_))
            logger.info(s"Session keys wiped for ${hc.sessionId}")
            val attemptsRemaining = viewConfig.retryLimit - attempt
            val origin = request.session.get("origin").getOrElse("")
            if(origin.isEmpty) logger.info(s"origin in session not present upon submission for ${hc.sessionId}")

            if (attempt < appConfig.retryLimit) {
              val isSA = origin == "bta-sa" || origin == "pta-sa" || origin == "ssttp-sa"
              val pdvFailedAttempt = PdvFailedAttempt(attempt, appConfig.retryLimit, personalDetails.journeyVersion, maybeCredId, origin)
              dataStreamAuditService.audit(pdvFailedAttempt)
              if (isSA) Redirect(routes.PersonalDetailsCollectionController.incorrectDetailsForSa(completionUrl, attemptsRemaining, failureUrl)).withSession(cleanedSession)
              else Redirect(routes.PersonalDetailsCollectionController.incorrectDetails(completionUrl, attemptsRemaining, failureUrl)).withSession(cleanedSession)
            } else {
              val pdvLockedOut = PdvLockedOut(personalDetails.journeyVersion, maybeCredId, origin)
              dataStreamAuditService.audit(pdvLockedOut)
              if (failureUrl.isDefined) {
                Redirect(failureUrl.get.value).withSession(cleanedSession)
              } else {
                Redirect(routes.PersonalDetailsCollectionController.lockedOut()).withSession(cleanedSession)
              }
            }
          } else {
            val cleanedSession = pdvSessionKeys.foldLeft(request.session)(_.-(_))
            logger.info(s"Session keys wiped for ${hc.sessionId}")
            Redirect(routes.PersonalDetailsCollectionController.enterYourDetails(completionUrl, withError = true, failureUrl)).withSession(cleanedSession)
          }
      }
    } yield result
  }.recover {
    case _: Exception =>
      if (appConfig.enabledCircuitBreaker) {
        Redirect(routes.PersonalDetailsCollectionController.serviceTemporarilyUnavailable())
      }
      else {
        val redirectUrl: String = if (failureUrl.isDefined) {failureUrl.get.value} else {completionUrl.value}
        Redirect(redirectUrl)
      }
  }



  /**
    * redirect user to the completionUrl with a timeout status
    */
  def redirectAfterTimeout(completionUrl: CompletionUrl, failureUrl: Option[CompletionUrl]): Action[AnyContent] = Action.async { implicit request =>
    ivConnector.updateJourney(completionUrl.value, "Timeout")
    val redirectUrl: String = if (failureUrl.isDefined) {failureUrl.get.value} else {completionUrl.value}
    Future.successful(Redirect(redirectUrl, Map("userTimeout" -> Seq(""))))
  }

  /**
   * redirect user to the completionUrl with a userAborted status
   */
  def redirectAfterUserAborted(completionUrl: CompletionUrl, failureUrl: Option[CompletionUrl]): Action[AnyContent] = Action.async { implicit request =>
    ivConnector.updateJourney(completionUrl.value, "UserAborted")
    Future.successful(Redirect(completionUrl.value, Map("userAborted" -> Seq(""))))
  }


  /**
    * Endpoint which just has the side-effect of extending the Play session to avoid (the 15 min) timeout
    *
    * */
  def keepAlive: Action[AnyContent] = Action.async {
    Future.successful(Ok("OK"))
  }

  def weCannotCheckYourIdentity(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(weCannotCheckYourIdentityPage()))
  }

  def incorrectDetails(completionUrl: CompletionUrl, attemptsRemaining: Int, failureUrl: Option[CompletionUrl]): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(incorrect_details(completionUrl, attemptsRemaining, isSA = false, failureUrl)))
  }

  def incorrectDetailsForSa(completionUrl: CompletionUrl, attemptsRemaining: Int, failureUrl: Option[CompletionUrl]): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(incorrect_details(completionUrl, attemptsRemaining, isSA = true, failureUrl)))
  }

  def contactTechnicalSupport(redirectUrl: String): Action[AnyContent] = Action { _ =>
    // need some event set up? eventDispatcher.dispatchEvent()
    Redirect(redirectUrl)
  }

  def redirectToHelplineServiceDeceasedPage(): Action[AnyContent] = Action.async { _ =>
    val helplineServiceDeceasedPageUrl = appConfig.helplineUrl + "/helpline/has-this-person-died"
    Future.successful(Redirect(helplineServiceDeceasedPageUrl))
  }

  def lockedOut(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(locked_out()))
  }

  def serviceTemporarilyUnavailable(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(service_temporarily_unavailable()))
  }

  def youHaveBeenTimedOut(failureUrl: Option[String]) : Action[AnyContent] = Action.async { implicit request =>
    viewConfig.isLoggedIn.map { isLoggedIn: Boolean =>
       if(failureUrl.isDefined) Ok(you_have_been_timed_out_dwp(loggedInUser = isLoggedIn, failureUrl))
       else Ok(you_have_been_timed_out(loggedInUser = isLoggedIn))
    }
  }

}
