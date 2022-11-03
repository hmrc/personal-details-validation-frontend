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

import akka.actor.ActorSystem
import akka.stream.Materializer
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.SEE_OTHER
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.mvc._
import play.api.test.FakeRequest
import support.Generators.Implicits._
import support.UnitSpec
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.config.{AppConfig, DwpMessagesApiProvider}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.personaldetailsvalidation.connectors.IdentityVerificationConnector
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators
import uk.gov.hmrc.personaldetailsvalidation.model.{CompletionUrl, UserAttemptsDetails}
import uk.gov.hmrc.personaldetailsvalidation.monitoring.{EventDispatcher, MonitoringEvent, PdvLockedOut}
import uk.gov.hmrc.personaldetailsvalidation.monitoring.dataStreamAudit.DataStreamAuditService
import uk.gov.hmrc.personaldetailsvalidation.views.html.pages.{incorrect_details, locked_out, service_temporarily_unavailable, we_cannot_check_your_identity, you_have_been_timed_out}
import uk.gov.hmrc.personaldetailsvalidation.views.html.template.{enter_your_details, what_is_your_nino, what_is_your_postcode}
import uk.gov.hmrc.views.ViewConfig

import scala.concurrent.{ExecutionContext, Future}

/*
 * Currently only iv will send failureURL to PDV for dwp and *-sa origins.
 * The failureUrl is optional.
 * Redirect the user to the url when pdv failed 5 times, without the url user will see a default lockout page (PersonalDetailsCollectionControllerSpec).
 */
class PDVControllerWithFailureUrlSpec extends UnitSpec with MockFactory with ScalaFutures with GuiceOneAppPerSuite {

  "start" should {

    "Redirect to lockedOut page when user tried 5 times and there is a failureUrl" in new Setup {

      (personalDetailsSubmitterMock.getUserAttempts()(_: HeaderCarrier))
        .expects(*)
        .returns(Future.successful(UserAttemptsDetails(5, None)))

      val pdvLockedOut: PdvLockedOut = PdvLockedOut("reattempt PDV within 24 hours", "", "")
      (mockDataStreamAuditService.audit(_: MonitoringEvent)(_: HeaderCarrier, _:ExecutionContext))
        .expects(pdvLockedOut, *, *)

      val result: Future[Result] = controller.showPage(completionUrl, None, failureUrl)(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(await(result)).get shouldBe failureUrl.get.toString
    }

  }

  private trait Setup {

    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

    val completionUrl: CompletionUrl = ValuesGenerators.completionUrls.generateOne
    val failureUrl: Option[CompletionUrl] = Some(ValuesGenerators.completionUrls.generateOne)

    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: Materializer = Materializer.apply(system)

    implicit val dwpMessagesApiProvider: DwpMessagesApiProvider = app.injector.instanceOf[DwpMessagesApiProvider]
    implicit val lang: Lang = Lang("en-GB")
    implicit val messages: Messages = MessagesImpl(lang, dwpMessagesApiProvider.get)

    val personalDetailsSubmitterMock: PersonalDetailsSubmission = mock[PersonalDetailsSubmission]
    val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
    val mockDataStreamAuditService: DataStreamAuditService = mock[DataStreamAuditService]
    val mockEventDispatcher: EventDispatcher = mock[EventDispatcher]
    val mockIVConnector: IdentityVerificationConnector = mock[IdentityVerificationConnector]
    implicit val mockViewConfig: ViewConfig = app.injector.instanceOf[ViewConfig]

    val stubMessagesControllerComponents: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

    val enter_your_details: enter_your_details = app.injector.instanceOf[enter_your_details]
    val incorrect_details: incorrect_details = app.injector.instanceOf[incorrect_details]
    val locked_out: locked_out = app.injector.instanceOf[locked_out]
    val what_is_your_postcode: what_is_your_postcode = app.injector.instanceOf[what_is_your_postcode]
    val what_is_your_nino: what_is_your_nino = app.injector.instanceOf[what_is_your_nino]
    val service_temporarily_unavailable: service_temporarily_unavailable = app.injector.instanceOf[service_temporarily_unavailable]
    val you_have_been_timed_out: you_have_been_timed_out = app.injector.instanceOf[you_have_been_timed_out]

    val we_cannot_check_your_identity: we_cannot_check_your_identity = app.injector.instanceOf[we_cannot_check_your_identity]

    implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

    implicit val authConnector: AuthConnector = app.injector.instanceOf[AuthConnector]

    implicit val ec: ExecutionContext = ExecutionContext.global

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val controller = new PersonalDetailsCollectionController(
      personalDetailsSubmitterMock,
      appConfig,
      mockDataStreamAuditService,
      mockEventDispatcher,
      stubMessagesControllerComponents,
      what_is_your_postcode,
      what_is_your_nino,
      enter_your_details,
      incorrect_details,
      locked_out,
      we_cannot_check_your_identity,
      service_temporarily_unavailable,
      you_have_been_timed_out,
      mockIVConnector)

  }
}
