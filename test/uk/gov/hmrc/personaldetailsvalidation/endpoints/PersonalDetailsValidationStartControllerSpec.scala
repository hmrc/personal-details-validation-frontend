/*
 * Copyright 2018 HM Revenue & Customs
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

import generators.Generators.Implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.mvc.Http.HeaderNames.LOCATION
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators
import uk.gov.hmrc.play.test.UnitSpec

import scalamock.MockArgumentMatchers

class PersonalDetailsValidationStartControllerSpec
  extends UnitSpec
    with MockFactory
    with MockArgumentMatchers
    with ScalaFutures {

  "start" should {

    "redirect to personal details page" in new Setup {
      val result = controller.start(completionUrl)(request)

      status(result) shouldBe SEE_OTHER

      header(LOCATION, result) shouldBe Some(routes.PersonalDetailsCollectionController.showPage(completionUrl).url)
    }
  }

  private trait Setup {

    protected implicit val request: Request[AnyContentAsEmpty.type] = FakeRequest()

    val completionUrl = ValuesGenerators.completionUrls.generateOne

    val controller = new PersonalDetailsValidationStartController()
  }
}
