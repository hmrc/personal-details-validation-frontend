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

package uk.gov.hmrc.personaldetailsvalidation.personaldetails.verifiers

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.mvc.Results._
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import setups.controllers.ResultVerifiers._
import uk.gov.hmrc.personaldetailsvalidation.generators.Generators.Implicits._
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators.journeyIds
import uk.gov.hmrc.personaldetailsvalidation.model.JourneyId
import uk.gov.hmrc.personaldetailsvalidation.personaldetails.repository.JourneyRepository
import uk.gov.hmrc.errorhandling.ErrorHandler
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}
import scalamock.MockArgumentMatchers

class JourneyIdVerifierSpec
  extends UnitSpec
    with ScalaFutures
    with MockFactory
    with MockArgumentMatchers {

  "forExisting" should {

    "return an Action which body will get evaluated " +
      "if the given journeyId exists in the repository" in new Setup {
      (journeyRepository.journeyExists(_: JourneyId)(_: ExecutionContext))
        .expects(journeyId, instanceOf[MdcLoggingExecutionContext])
        .returning(Future.successful(true))

      verifier.forExisting(journeyId).async(actionBody)(request).futureValue shouldBe actionBodyResult
    }

    "return an Action which body won't get evaluated " +
      "but will always return NOT_FOUND with the Technical Error page " +
      "if the given journeyId does not exist in the repository" in new Setup {
      (journeyRepository.journeyExists(_: JourneyId)(_: ExecutionContext))
        .expects(journeyId, instanceOf[MdcLoggingExecutionContext])
        .returning(Future.successful(false))

      (errorHandler.internalServerErrorTemplate(_: Request[_]))
        .expects(request)
        .returning(Html("technical error page"))

      val result = verifier.forExisting(journeyId).async(actionBody)(request)

      verify(result).has(statusCode = NOT_FOUND, content = "technical error page")
    }
  }

  private trait Setup {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

    val journeyId: JourneyId = journeyIds.generateOne

    val actionBodyResult: Result = Ok("html")
    val actionBody: Request[_] => Future[Result] = _ => Future.successful(actionBodyResult)

    val errorHandler: ErrorHandler = mock[ErrorHandler]
    val journeyRepository: JourneyRepository = mock[JourneyRepository]

    val verifier = new JourneyIdVerifier(errorHandler, journeyRepository)
  }
}
