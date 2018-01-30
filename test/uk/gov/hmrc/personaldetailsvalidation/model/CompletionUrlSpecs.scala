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

package uk.gov.hmrc.personaldetailsvalidation.model

import cats.implicits._
import uk.gov.hmrc.personaldetailsvalidation.model.CompletionUrl.completionUrl
import uk.gov.hmrc.play.test.UnitSpec

class CompletionUrlSpecs extends UnitSpec {

  "completionUrl" should {

    "be instantiatable if the value does start with '/'" in {
      completionUrl("/foo").map(_.value) shouldBe Right("/foo")
    }

    "be instantiatable if the value does start with 'http://localhost'" in {
      completionUrl("http://localhost/foo").map(_.value) shouldBe Right("http://localhost/foo")
    }

    "not be instantiatable if the value neither starts with '/' nor 'http://localhost'" in {
      val Left(exception) = completionUrl("foobar")
      exception shouldBe a[IllegalArgumentException]
    }

    "not be instantiatable if the value contains '//' and does not start with 'http://localhost'" in {
      val Left(exception) = completionUrl("/foobar//baz")
      exception shouldBe a[IllegalArgumentException]
    }
  }
}
