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

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{Action, AnyContent, DefaultMessagesControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.config.{AppConfig, DwpMessagesApiProvider}
import uk.gov.hmrc.personaldetailsvalidation.connectors.IdentityVerificationConnector
import support.Generators.Implicits._
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators
import uk.gov.hmrc.personaldetailsvalidation.model.CompletionUrl
import uk.gov.hmrc.personaldetailsvalidation.monitoring.EventDispatcher
import uk.gov.hmrc.personaldetailsvalidation.views.html.pages.we_cannot_check_your_identity
import uk.gov.hmrc.personaldetailsvalidation.views.html.template.{enter_your_details, what_is_your_nino, what_is_your_postcode}
import support.UnitSpec
import setups.views.ViewSetup
import uk.gov.hmrc.scalatestaccessibilitylinter.AccessibilityMatchers
import scala.concurrent.ExecutionContext


class AllySpec extends UnitSpec with GuiceOneAppPerSuite with AccessibilityMatchers {

  val isLoggedInUser = true

  val url: CompletionUrl = ValuesGenerators.completionUrls.generateOne

  "render" should {

    "return the nino page" in new Setup {

      val document: Action[AnyContent] = controller.whatIsYourNino(url)
      document.toString() should passAccessibilityChecks
    }
  }

  private trait Setup extends ViewSetup {

    lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

    implicit val mockDwpMessagesApi = app.injector.instanceOf[DwpMessagesApiProvider]
    implicit val authConnector = app.injector.instanceOf[AuthConnector]
    implicit val executionContext = app.injector.instanceOf[ExecutionContext]

    private val personalDetailsSubmission: PersonalDetailsSubmission = app.injector.instanceOf[PersonalDetailsSubmission]
    private val eventDispatcher: EventDispatcher = app.injector.instanceOf[EventDispatcher]
    private val controllerComponents: DefaultMessagesControllerComponents = app.injector.instanceOf[DefaultMessagesControllerComponents]
    private val enter_your_details: enter_your_details = app.injector.instanceOf[enter_your_details]
    private val what_is_your_nino: what_is_your_nino = app.injector.instanceOf[what_is_your_nino]
    private val what_is_your_postcode: what_is_your_postcode = app.injector.instanceOf[what_is_your_postcode]
    private val we_cannot_check_your_identity: we_cannot_check_your_identity = app.injector.instanceOf[we_cannot_check_your_identity]
    private val ivConnector: IdentityVerificationConnector = app.injector.instanceOf[IdentityVerificationConnector]

    val controller: PersonalDetailsCollectionController =
      new PersonalDetailsCollectionController(
        personalDetailsSubmission,
        appConfig,
        eventDispatcher,
        controllerComponents,
        what_is_your_postcode,
        what_is_your_nino,
        enter_your_details,
        we_cannot_check_your_identity,
        ivConnector)(authConnector, mockDwpMessagesApi, viewConfig, executionContext, messagesApi)
  }

}
