/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.voa.valuetype

import uk.gov.voa.valuetype.tooling.UnitSpec

class IntOptionsSpec extends UnitSpec {

  import TestIntOption._

  "all" should {

    "return all items" in {
      TestIntOption.all should contain theSameElementsAs Seq(TestOption5, TestOption6, TestOption7)
    }
  }

  "get" should {

    "return the matching option" in {
      TestIntOption.get(6) shouldBe Some(TestOption6)
    }

    "return None if the given Int does not match the value of any option" in {
      TestIntOption.get(0) shouldBe None
    }
  }

  "of" should {

    "return the matching option" in {
      TestIntOption.of(7) shouldBe TestOption7
    }

    "throw an exception if the given Int does not match the value of any option" in {
      an[IllegalArgumentException] should be thrownBy TestIntOption.of(8)
    }
  }
}
