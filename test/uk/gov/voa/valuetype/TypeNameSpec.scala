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

private case class NotNestedType(value: String) extends StringValue

class TypeNameSpec extends UnitSpec {

  "TypeName" should {

    "provide type name" in {
      NotNestedType("abc").typeName shouldBe "NotNestedType"
    }

    "provide type name without dollar sign for nested types" in {
      case class NestedType(value: String) extends StringValue

      NestedType("abc").typeName shouldBe "NestedType"
    }

  }
}
