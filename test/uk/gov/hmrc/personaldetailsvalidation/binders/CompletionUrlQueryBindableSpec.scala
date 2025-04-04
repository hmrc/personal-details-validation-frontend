/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.personaldetailsvalidation.binders

import java.net.URLEncoder

import uk.gov.hmrc.personaldetailsvalidation.model.CompletionUrl
import support.UnitSpec

class CompletionUrlQueryBindableSpec extends UnitSpec {

  "completionUrlQueryBindable.bind" should {

    val key = "completionUrl"

    "return CompletionUrl if key is present in params" in {
      val url = "/foo/bar"

      completionUrlQueryBinder.bind(key, Map(key -> Seq(url))) match {
        case Some(Right(completionUrl)) => completionUrl.value shouldBe url
        case other => fail(s"Error bind returned unexpected value : $other")
      }

    }

    "return None if key is not present" in {
      completionUrlQueryBinder.bind(key, Map.empty) shouldBe None
    }

    "return error if key not a valid completion url" in {
      val url = "foo/bar"

      completionUrlQueryBinder.bind(key, Map(key -> Seq(url.toString))) match {
        case Some(Left(errMsg)) => errMsg shouldBe s"$url is not a relative url"
        case other => fail(s"Error bind returned unexpected value : $other")
      }
    }
  }

  "completionUrlQueryBindable.unbind" should {

    val key = "completionUrl"

    "return query param string" in {
      val url = "/foo/bar?param1=value1&param2=value2"

      CompletionUrl.completionUrl(url) match {
        case Right(completionUrl) => completionUrlQueryBinder.unbind(key, completionUrl) shouldBe s"$key=${URLEncoder.encode(url, "UTF-8")}"
        case _ => fail("Error : The Url is invalid")
      }
    }
  }
}
