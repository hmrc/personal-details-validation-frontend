/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.personaldetailsvalidationfrontend.personaldetails

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.mvc.Request
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.personaldetailsvalidationfrontend.generators.Generators.Implicits._
import uk.gov.hmrc.personaldetailsvalidationfrontend.generators.ValuesGenerators.journeyIds
import uk.gov.hmrc.personaldetailsvalidationfrontend.personaldetails.views.pages.PersonalDetailsPage
import uk.gov.hmrc.personaldetailsvalidationfrontend.test.controllers.EndpointRequiringBodySetup
import uk.gov.hmrc.play.test.UnitSpec

class PersonalDetailsCollectionControllerSpec
  extends UnitSpec
    with MockFactory
    with ScalaFutures {

  "show" should {
    "return OK with body rendered using PersonalDetailsPage" in new Setup {
      (page.render(_: Request[_]))
        .expects(request)
        .returning(Html("content"))

      val result = controller.showPage(journeyId)(request)

      status(result) shouldBe OK
      contentType(result) shouldBe Some(HTML)
      charset(result) shouldBe Some("utf-8")
      bodyOf(result).futureValue shouldBe "content"
    }
  }

  private trait Setup extends EndpointRequiringBodySetup {
    val journeyId = journeyIds.generateOne
    val page: PersonalDetailsPage = mock[PersonalDetailsPage]
    val controller = new PersonalDetailsCollectionController(page)
  }
}
