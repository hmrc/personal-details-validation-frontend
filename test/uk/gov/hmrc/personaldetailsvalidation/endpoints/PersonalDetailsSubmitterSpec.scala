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

package uk.gov.hmrc.personaldetailsvalidation.endpoints

import cats.Id
import generators.Generators.Implicits._
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import play.api.mvc.Request
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.personaldetailsvalidation.connectors.PersonalDetailsValidationConnector
import uk.gov.hmrc.personaldetailsvalidation.generators.ObjectGenerators.personalDetailsObjects
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators.completionUrls
import uk.gov.hmrc.personaldetailsvalidation.model.PersonalDetails
import uk.gov.hmrc.personaldetailsvalidation.views.pages.PersonalDetailsPage
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.{global => executionContext}

class PersonalDetailsSubmitterSpec
  extends UnitSpec
    with MockFactory {

  "bindAndSend" should {

    "return BAD_REQUEST when form binds with errors" in new Setup {
      (page.bind(_: Request[_]))
        .expects(request)
        .returning(Left(BadRequest("page with errors")))

      val result = submitter.bindAndSend(completionUrl)

      status(result) shouldBe BAD_REQUEST
    }

    "returns redirect to the completionUrl with appended validationId " +
      "returned from passing successfully bound Personal Details to the validation service" in new Setup {
      val personalDetails = personalDetailsObjects.generateOne

      (page.bind(_: Request[_]))
        .expects(request)
        .returning(Right(personalDetails))

      val validationId = Gen.uuid.generateOne.toString
      (personalDetailsValidationConnector.passToValidation(_: PersonalDetails)(_: HeaderCarrier, _: ExecutionContext))
        .expects(personalDetails, headerCarrier, executionContext)
        .returning(validationId)

      val result = submitter.bindAndSend(completionUrl)

      status(result) shouldBe SEE_OTHER
      result.header.headers(LOCATION) shouldBe s"$completionUrl?validationId=$validationId"
    }
  }

  private trait Setup {
    implicit val request = FakeRequest()
    implicit val headerCarrier = HeaderCarrier()

    val completionUrl = completionUrls.generateOne

    val page = mock[PersonalDetailsPage]

    abstract class ConnectorInterpretation extends PersonalDetailsValidationConnector[Id]
    val personalDetailsValidationConnector = mock[ConnectorInterpretation]

    val submitter = new PersonalDetailsSubmitter[Id](page, personalDetailsValidationConnector)
  }
}
