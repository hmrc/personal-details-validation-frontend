/*
 * Copyright 2019 HM Revenue & Customs
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

import java.util.UUID

import generators.Generators.Implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.mvc.Results._
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import setups.controllers.ResultVerifiers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators
import uk.gov.hmrc.personaldetailsvalidation.model.CompletionUrl
import uk.gov.hmrc.personaldetailsvalidation.views.pages.PersonalDetailsPage
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext
import support.UnitSpec
import scala.concurrent.duration._

import scala.concurrent.{Await, ExecutionContext, Future}
import scalamock.MockArgumentMatchers

class PersonalDetailsCollectionControllerSpec
  extends UnitSpec
    with MockFactory
    with MockArgumentMatchers
    with ScalaFutures {

  "showPage" should {

    "return OK with HTML body rendered using PersonalDetailsPage" in new Setup {
      (page.render(_: Boolean)(_: CompletionUrl, _: Request[_]))
        .expects(false, completionUrl, request)
        .returning(Html("content"))

      val result = controller.showPage(completionUrl, alternativeVersion = false)(request)

      verify(result).has(statusCode = OK, content = "content")
    }
  }

  "submit" should {

    "pass the outcome of bindValidateAndRedirect" in new Setup {

      val redirectUrl = s"${completionUrl.value}?validationId=${UUID.randomUUID()}"

      (personalDetailsSubmitter.submit(_: CompletionUrl, _: Boolean)(_: Request[_], _: HeaderCarrier, _: ExecutionContext))
        .expects(completionUrl, false, request, instanceOf[HeaderCarrier], instanceOf[MdcLoggingExecutionContext])
        .returning(Future.successful(Redirect(redirectUrl)))

      val result = controller.submit(completionUrl, alternativeVersion = false)(request)

      redirectLocation(Await.result(result, 5 seconds)) shouldBe Some(redirectUrl)
    }
  }

  private trait Setup {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

    val completionUrl = ValuesGenerators.completionUrls.generateOne

    val page: PersonalDetailsPage = mock[PersonalDetailsPage]

    val personalDetailsSubmitter = mock[FuturedPersonalDetailsSubmission]

    val controller = new PersonalDetailsCollectionController(page, personalDetailsSubmitter)
  }
}
