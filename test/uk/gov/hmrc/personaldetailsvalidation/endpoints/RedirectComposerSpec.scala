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
import generators.Generators.nonEmptyStrings
import org.scalatest.prop.{TableDrivenPropertyChecks, Tables}
import play.api.test.Helpers._
import uk.gov.hmrc.personaldetailsvalidation.model.CompletionUrl
import uk.gov.hmrc.personaldetailsvalidation.model.CompletionUrl.{completionUrl => newCompletionUrl}
import uk.gov.hmrc.play.test.UnitSpec

import scala.language.implicitConversions

class RedirectComposerSpec
  extends UnitSpec
    with TableDrivenPropertyChecks {

  private val validationId = nonEmptyStrings.generateOne
  private val scenarios = Tables.Table(
    ("given completion url",        "expected redirect url"),
    ("/some-url",                   s"/some-url?validationId=$validationId"),
    ("/some-url?parameter1=value1", s"/some-url?parameter1=value1&validationId=$validationId")
  )

  "compose" should {

    forAll(scenarios) { (completionUrl, expectedRedirect) =>

      s"return a Redirect to the $expectedRedirect when completionUrl is '$completionUrl'" in {

        val result = new RedirectComposer().compose(completionUrl, validationId)

        status(result) shouldBe SEE_OTHER
        result.header.headers.get(LOCATION) shouldBe Some(expectedRedirect)
      }
    }
  }

  private implicit def toCompletionUrl(completionUrl: String): CompletionUrl =
    newCompletionUrl(completionUrl).fold(throw _, identity)
}
