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

import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.personaldetailsvalidation.model.CompletionUrl

private class RedirectComposer {

  private val validationIdQueryParameter = "validationId"
  private val technicalErrorQueryParameter = "technicalError"

  def redirect(completionUrl: CompletionUrl, validationId: String): Result =
    Redirect(completionUrl appendParameter validationId.toQueryParameter)

  def redirectWithTechnicalErrorParameter(completionUrl: CompletionUrl): Result =
    Redirect(completionUrl appendParameter technicalErrorQueryParameter)

  private implicit class CompletionUrlOps(completionUrl: CompletionUrl) {

    def appendParameter(parameter: String) = Option(new URI(completionUrl.value).getQuery) match {
      case None => s"$completionUrl?$parameter"
      case _ => s"$completionUrl&$parameter"
    }
  }

  private implicit class QueryParameterOps(validationId: String) {
    val toQueryParameter: String = s"$validationIdQueryParameter=$validationId"
  }
}