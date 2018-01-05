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

import java.net.URI

import cats.Id
import generators.Generators.Implicits._
import generators.Generators._
import org.scalamock.scalatest.MockFactory
import org.scalatest.prop.{TableDrivenPropertyChecks, Tables}
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.personaldetailsvalidation.connectors.{PersonalDetailsSender, ValidationIdFetcher}
import uk.gov.hmrc.personaldetailsvalidation.generators.ObjectGenerators.personalDetailsObjects
import uk.gov.hmrc.personaldetailsvalidation.generators.ValuesGenerators.{completionUrls, uris}
import uk.gov.hmrc.personaldetailsvalidation.model.{CompletionUrl, PersonalDetails}
import uk.gov.hmrc.personaldetailsvalidation.views.pages.PersonalDetailsPage
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.{global => executionContext}

class PersonalDetailsSubmissionSpec
  extends UnitSpec
    with MockFactory
    with TableDrivenPropertyChecks {

  "bindValidateAndRedirect" should {

    "return BAD_REQUEST when form binds with errors" in new Setup {

      val completionUrl = completionUrls.generateOne

      (page.bindFromRequest(_: Request[_], _: CompletionUrl))
        .expects(request, completionUrl)
        .returning(Left(Html("page with errors")))

      val result = submitter.bindValidateAndRedirect(completionUrl)

      status(result) shouldBe BAD_REQUEST
      contentAsString(result) shouldBe "page with errors"
    }


    "bind the request to PersonalDetails, " +
      "post it to the validation service, " +
      "fetch validationId from the validation service and " +
      "return redirect to completionUrl with appended validationId query parameter" in new Setup with HappyTestCaseScenarios {

      forAll(completionUrlScenarios) { case (completionUrl, validationId, expectedRedirectUrl) =>

        val personalDetails = personalDetailsObjects.generateOne
        (page.bindFromRequest(_: Request[_], _: CompletionUrl))
          .expects(request, completionUrl)
          .returning(Right(personalDetails))

        val locationUrl = uris.generateOne
        (personalDetailsValidationConnector.passToValidation(_: PersonalDetails)(_: HeaderCarrier, _: ExecutionContext))
          .expects(personalDetails, headerCarrier, executionContext)
          .returning(locationUrl)

        (validationIdFetcher.fetchValidationId(_: URI)(_: HeaderCarrier, _: ExecutionContext))
          .expects(locationUrl, headerCarrier, executionContext)
          .returning(validationId)

        val result = submitter.bindValidateAndRedirect(completionUrl)

        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) shouldBe expectedRedirectUrl
      }
    }
  }

  private trait Setup {
    implicit val request: Request[AnyContentAsEmpty.type] = FakeRequest()
    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

    val page = mock[PersonalDetailsPage]
    val personalDetailsValidationConnector = mock[PersonalDetailsSender[Id]]
    val validationIdFetcher = mock[ValidationIdFetcher[Id]]
    val submitter = new PersonalDetailsSubmission[Id](page, personalDetailsValidationConnector, validationIdFetcher)
  }

  private trait HappyTestCaseScenarios {

    val completionUrlScenarios = Tables.Table(
      ("given completion url", "validationId", "expected redirect url"),
      row(),
      row(withCompletionUrlQueryParameters = "param=value")
    )

    private def row(withCompletionUrlQueryParameters: String*): (CompletionUrl, String, String) = {
      for {
        completionUrl <- completionUrls
        validationId <- nonEmptyStrings
      } yield {
        val completionUrlWithQueryParams = completionUrl.copy(s"${completionUrl.value}${withCompletionUrlQueryParameters.asUrlPart}")
        val redirectUrlQueryParams = (withCompletionUrlQueryParameters :+ s"validationId=$validationId").asUrlPart
        (completionUrlWithQueryParams, validationId, s"$completionUrl$redirectUrlQueryParams")
      }
    }.generateOne

    private implicit class QueryParams(queryParams: Seq[String]) {
      lazy val asUrlPart: String = queryParams match {
        case Nil => ""
        case params => s"?${params.mkString("&")}"
      }
    }
  }
}