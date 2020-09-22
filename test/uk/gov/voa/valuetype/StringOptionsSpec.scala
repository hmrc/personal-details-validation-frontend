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

class StringOptionsSpec extends UnitSpec {

  import TestStringOption._

  "typeName" should {

    "provide type name" in {
      TestStringOption.typeName shouldBe "TestStringOption"
    }

  }

  "all" should {

    "return all items" in {
      TestStringOption.all should contain theSameElementsAs Seq(TestOption1, TestOption2)
    }
  }

  "get" should {

    "return the matching option" in {
      TestStringOption.get("1") shouldBe Some(TestOption1)
    }

    "return None if given String does not match the value of any option" in {
      TestStringOption.get("invalid") shouldBe None
    }
  }

  "of" should {

    "return the matching option" in {
      TestStringOption.of("2") shouldBe TestOption2
    }

    "throw an exception if the given String does not match the value of any option" in {
      an[IllegalArgumentException] should be thrownBy TestStringOption.of("invalid")
    }
  }
}
