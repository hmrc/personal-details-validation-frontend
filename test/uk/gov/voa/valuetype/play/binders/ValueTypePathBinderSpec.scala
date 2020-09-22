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

package uk.gov.voa.valuetype.play.binders

import play.api.mvc.PathBindable
import uk.gov.voa.valuetype.TestIntValue
import uk.gov.voa.valuetype.tooling.UnitSpec

import scala.util.Try

class ValueTypePathBinderSpec extends UnitSpec {

  "ValueTypePathBinder.bind" should {

    "bind successfully to relevant ValueType if given String value is parsable" in new BindingTestContext {
      intValueBinder.bind("key", "1") shouldBe Right(TestIntValue(1))
    }

    "bind with an error if given String value is not parsable" in new BindingTestContext {
      intValueBinder.bind("key", "a") shouldBe a[Left[_, _]]
    }
  }

  "ValueTypePathBinder.unbind" should {

    "allow to unbind the given ValueType to a String" in new BindingTestContext {
      intValueBinder.unbind("key", TestIntValue(1)) shouldBe "1"
    }
  }

  private trait BindingTestContext extends ValueTypePathBinder {
    implicit val intValueParser: (String) => Try[TestIntValue] = value => Try {
      TestIntValue(value.toInt)
    }

    val intValueBinder = implicitly[PathBindable[TestIntValue]]
  }

}
