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

package uk.gov.hmrc.personaldetailsvalidation.binders

import uk.gov.hmrc.personaldetailsvalidation.model.RelativeUrl
import uk.gov.hmrc.play.test.UnitSpec

class RelativeUrlQueryBindableSpec extends UnitSpec {

  "relativeUrlQueryBindable.bind" should {

    val key = "completionUrl"

    "return RelativeUrl if key is present in params" in {
      val url = "/foo/bar"

      val Some(Right(relativeUrl)) = relativeUrlQueryBinder.bind(key, Map(key -> Seq(url.toString)))

      relativeUrl.value shouldBe url
    }

    "return None if key is not present" in {
      relativeUrlQueryBinder.bind(key, Map.empty) shouldBe None
    }

    "return error if key not a valid relative url" in {
      val url = "foo/bar"

      val result = relativeUrlQueryBinder.bind(key, Map(key -> Seq(url.toString)))

      result shouldBe Some(Left(s"$url is not a relative url"))
    }
  }

  "relativeUrlQueryBindable.unbind" should {

    val key = "completionUrl"

    "return query param string" in {
      val url = "/foo/bar"
      val Right(relativeUrl) = RelativeUrl.relativeUrl(url)
      relativeUrlQueryBinder.unbind(key, relativeUrl) shouldBe s"$key=$url"
    }
  }
}
