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

package uk.gov.hmrc.personaldetailsvalidationfrontend.model

import uk.gov.hmrc.play.test.UnitSpec

class RelativeUrlSpecs extends UnitSpec {

  "RelativeUrl" should {
    "not be allowed to be constructed if parameter value does not start with '/'" in {
      val Left(exception) =  RelativeUrl.relativeUrl("foobar")
      exception shouldBe a[IllegalArgumentException]
    }

    "not be allowed to be constructed if parameter value contains '//'" in {
      val Left(exception) =  RelativeUrl.relativeUrl("/foobar//baz")
      exception shouldBe a[IllegalArgumentException]
    }
  }

}
