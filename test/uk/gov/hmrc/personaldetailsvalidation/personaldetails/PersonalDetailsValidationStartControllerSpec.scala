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

package uk.gov.hmrc.personaldetailsvalidation.personaldetails

import java.util.UUID
import java.util.UUID.randomUUID

import akka.Done
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.mvc.Http.HeaderNames.LOCATION
import uk.gov.hmrc.personaldetailsvalidation.model.{JourneyId, RelativeUrl}
import uk.gov.hmrc.personaldetailsvalidation.repository.JourneyRepository
import uk.gov.hmrc.personaldetailsvalidation.uuid.UUIDProvider
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}
import scalamock.MockArgumentMatchers

class PersonalDetailsValidationStartControllerSpec
  extends UnitSpec
    with MockFactory
    with MockArgumentMatchers
    with ScalaFutures {

  "start" should {

    "redirect to personal details page " +
      "when persistence of journeyId and relativeUrl is successful" in new Setup {
      (journeyRepository.persist(_: (JourneyId, RelativeUrl))(_: ExecutionContext))
        .expects(journeyId -> relativeUrl, instanceOf[MdcLoggingExecutionContext])
        .returning(Future.successful(Done))

      val result = controller.start(relativeUrl)(request)

      status(result) shouldBe SEE_OTHER

      header(LOCATION, result) shouldBe Some(routes.PersonalDetailsCollectionController.showPage(journeyId).url)
    }

    "fail when persisting journeyId and relativeUrl throws an exception" in new Setup {
      (journeyRepository.persist(_: (JourneyId, RelativeUrl))(_: ExecutionContext))
        .expects(journeyId -> relativeUrl, instanceOf[MdcLoggingExecutionContext])
        .returning(Future.failed(new RuntimeException("error")))

      a[RuntimeException] should be thrownBy controller.start(relativeUrl)(request).futureValue
    }
  }

  private trait Setup {

    protected implicit val request: Request[AnyContentAsEmpty.type] = FakeRequest()

    implicit val uuidProvider: UUIDProvider = stub[UUIDProvider]
    val journeyRepository: JourneyRepository = mock[JourneyRepository]

    val journeyIdValue: UUID = randomUUID()
    val journeyId = JourneyId(journeyIdValue)
    val Right(relativeUrl) = RelativeUrl.relativeUrl("/foo/bar")

    uuidProvider.apply _ when() returns journeyIdValue

    val controller = new PersonalDetailsValidationStartController(journeyRepository)
  }
}
