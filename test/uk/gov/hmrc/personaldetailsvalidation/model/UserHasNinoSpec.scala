/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.personaldetailsvalidation.model

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi}
import support.UnitSpec

class UserHasNinoSpec extends UnitSpec with GuiceOneAppPerSuite {

  private lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  private implicit lazy val messages: Messages = messagesApi.preferred(Seq(Lang("en")))

  "DoYouHaveYourNino form" should {

    "bind successfully when the answer is 'yes'" in {
      val form = DoYouHaveYourNino()
      val bound = form.bind(Map("do_you_have_your_nino" -> "yes"))

      bound.errors shouldBe Nil
      bound.value  shouldBe Some(UserHasNinoTrue)
    }

    "bind successfully when the answer is 'no'" in {
      val form = DoYouHaveYourNino()
      val bound = form.bind(Map("do_you_have_your_nino" -> "no"))

      bound.errors shouldBe Nil
      bound.value  shouldBe Some(UserHasNinoFalse)
    }

    "return a form error when no value is submitted" in {
      val form = DoYouHaveYourNino()
      val bound = form.bind(Map.empty[String, String])

      bound.errors should not be empty
      bound.errors.head.key shouldBe "do_you_have_your_nino"
    }

    "return a form error when an unrecognised value is submitted" in {
      val form = DoYouHaveYourNino()
      val bound = form.bind(Map("do_you_have_your_nino" -> "maybe"))

      bound.errors should not be empty
      bound.errors.head.key shouldBe "do_you_have_your_nino"
    }
  }
}
