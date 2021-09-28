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

import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import scalamock.MockArgumentMatchers
import support.UnitSpec
import uk.gov.hmrc.config.AppConfig

import scala.concurrent.{ExecutionContext, Future}

class SignOutControllerSpec extends UnitSpec with MockFactory with MockArgumentMatchers with GuiceOneAppPerSuite {

  val mcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val controller = new SignOutController(mcc)(appConfig, ExecutionContext.global)

  "SignOut Controller" should {
    "Redirect to logout" in {

      val result: Future[Result]  = controller.signOut().apply(FakeRequest())

      val expectedRedirectLocation =
        Some("http://localhost:9553/bas-gateway/sign-out-without-state?continue=http%3A%2F%2Flocalhost%3A9968%2Fpersonal-details-validation%2Fsigned-out&origin=pve")

      status(await(result)) shouldBe 303
      redirectLocation(await(result)) shouldBe expectedRedirectLocation
    }

    "Redirect to the logout page" in {
      val result = controller.signedOut().apply(FakeRequest())

      val expectedRedirectLocation = Some("https://www.access.service.gov.uk/logout")

      status(await(result)) shouldBe 303
      redirectLocation(await(result)) shouldBe expectedRedirectLocation

    }
  }
}