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

import java.util.UUID
import java.util.UUID.randomUUID

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.mvc.Http.HeaderNames.LOCATION
import uk.gov.hmrc.personaldetailsvalidationfrontend.personaldetails.model.{JourneyId, RelativeUrl}
import uk.gov.hmrc.personaldetailsvalidationfrontend.test.controllers.EndpointSetup
import uk.gov.hmrc.personaldetailsvalidationfrontend.uuid.UUIDProvider
import uk.gov.hmrc.play.test.UnitSpec

class PersonalDetailsValidationStartControllerSpec extends UnitSpec with ScalaFutures with MockFactory {

  "start" should {
    "redirect to personal details page" in new Setup {
      val result = controller.start(RelativeUrl("/foo/bar"))(request).futureValue
      status(result) shouldBe 303

      result.header.headers(LOCATION) shouldBe routes.PersonalDetailsCollectionController.showPage(JourneyId(journeyIdValue)).url
    }
  }

  trait Setup extends EndpointSetup {

    implicit val uuidProvider = stub[UUIDProvider]

    val journeyIdValue: UUID = randomUUID()

    uuidProvider.apply _ when() returns journeyIdValue

    val controller = new PersonalDetailsValidationStartController()
  }

}
