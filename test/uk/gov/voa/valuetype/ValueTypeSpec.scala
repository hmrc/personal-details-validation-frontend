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

import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import uk.gov.voa.valuetype.tooling.UnitSpec

import scala.math.BigDecimal.RoundingMode

class ValueTypeSpec extends UnitSpec {

  "ValueType" should {

    "provide type name" in {
      case class NestedType(value: Int) extends ValueType[Int]

      NestedType(1).typeName shouldBe "NestedType"

    }

  }
}

class StringValueSpec extends UnitSpec with PropertyChecks {

  val generatedStrings = Gen.alphaStr

  "StringValue.apply" should {

    "work for any String" in {
      forAll(generatedStrings) { value =>
        TestStringValue(value).value shouldBe value
      }
    }
  }

  "StringValue.toString" should {

    "be the same as the given value" in {
      forAll(generatedStrings) { value =>
        TestStringValue(value).toString shouldBe value
      }
    }
  }
}

class IntValueSpec extends UnitSpec with PropertyChecks {

  val generatedInts = Gen.choose(Int.MinValue, Int.MaxValue)

  "IntValue.apply" should {

    "work for any Int" in {
      forAll(generatedInts) { value =>
        TestIntValue(value).value shouldBe value
      }
    }
  }

  "IntValue.toString" should {

    "be the same as String representation of the given Int value" in {
      forAll(generatedInts) { value =>
        TestIntValue(value).toString shouldBe value.toString
      }
    }
  }
}

class LongValueSpec extends UnitSpec with PropertyChecks {

  val generatedLongs = Gen.choose(Long.MinValue, Long.MaxValue)

  "LongValue.apply" should {

    "work for any Long" in {
      forAll(generatedLongs) { value =>
        TestLongValue(value).value shouldBe value
      }
    }
  }

  "LongValue.toString" should {

    "be the same as String representation of the given Long value" in {
      forAll(generatedLongs) { value =>
        TestLongValue(value).toString shouldBe value.toString
      }
    }
  }
}

class BooleanValueSpec extends UnitSpec with PropertyChecks {

  val booleans = Table("Boolean", true, false)

  "BooleanValue.apply" should {

    "work for any Boolean" in {
      forAll(booleans) { value =>
        TestBooleanValue(value).value shouldBe value
      }
    }
  }

  "BooleanValue.toString" should {

    "be the same as String representation of the given Boolean value" in {
      forAll(booleans) { value =>
        TestBooleanValue(value).toString shouldBe value.toString
      }
    }
  }
}

class BigDecimalValueSpec extends UnitSpec with PropertyChecks {

  val generatedBigDecimals = arbitrary[BigDecimal]

  "BigDecimalValue.apply" should {

    "work with all given BigDecimal values" in {
      forAll(generatedBigDecimals) { value =>
        TestBigDecimalValue(value).value shouldBe value
      }
    }
  }

  "BooleanValue.toString" should {

    "be the same as String representation of the given Boolean value" in {
      forAll(generatedBigDecimals) { value =>
        TestBigDecimalValue(value).toString shouldBe value.toString
      }
    }
  }
}

class RoundedBigDecimalValueSpec extends UnitSpec with PropertyChecks {

  case class AnotherRoundedBigDecimalValue(nonRoundedValue: BigDecimal) extends RoundedBigDecimalValue {

    protected[this] def isOfThisInstance(other: RoundedBigDecimalValue) =
      other.isInstanceOf[AnotherRoundedBigDecimalValue]
  }

  val generatedBigDecimals = arbitrary[Double].map(BigDecimal.apply)

  "BigDecimalValue.apply" should {

    "work with all given BigDecimal values" in {
      forAll(generatedBigDecimals) { value =>
        TestRoundedBigDecimalValue(value).value shouldBe value.setScale(2, RoundingMode.HALF_UP)
      }
    }
  }

  "BigDecimalValue.value" should {

    "be initial value rounded half up using the scale of 2" in {
      TestRoundedBigDecimalValue(2.005).value shouldBe BigDecimal(2.01)
      TestRoundedBigDecimalValue(2.0049).value shouldBe BigDecimal(2.00)
    }
  }

  "BigDecimalValue.toString" should {

    "be the same as the given value" in {
      forAll(generatedBigDecimals) { value =>
        TestRoundedBigDecimalValue(value).toString shouldBe value.setScale(2, RoundingMode.HALF_UP).toString()
      }
    }
  }

  "BigDecimalValue.equals" should {

    "use the rounded value for checking equality" in {
      TestRoundedBigDecimalValue(2.005) shouldBe TestRoundedBigDecimalValue(2.01)
      TestRoundedBigDecimalValue(2.0049) shouldBe TestRoundedBigDecimalValue(2.00)
    }

    "return false if compared with different type of BigDecimalValue" in {
      TestRoundedBigDecimalValue(2.005) should not be AnotherRoundedBigDecimalValue(2.01)
      TestRoundedBigDecimalValue(2.005) should not be AnotherRoundedBigDecimalValue(2.005)
    }

    "return false if compared with a non BigDecimalValue" in {
      TestRoundedBigDecimalValue(2) should not be BigDecimal(2.00)
    }
  }

  "BigDecimalValue.hashCode" should {

    "return the same values for different instances representing the same value" in {
      TestRoundedBigDecimalValue(2.005).hashCode shouldBe TestRoundedBigDecimalValue(2.01).hashCode
      TestRoundedBigDecimalValue(2.0049).hashCode shouldBe TestRoundedBigDecimalValue(2.00).hashCode
    }

    "return different values for different instances representing different values" in {
      TestRoundedBigDecimalValue(2.05).hashCode should not be TestRoundedBigDecimalValue(2.00).hashCode
    }
  }

  "BigDecimalValue.==" should {

    "return true if compared with another instance of the same type representing the same value" in {
      TestRoundedBigDecimalValue(2.005) == TestRoundedBigDecimalValue(2.01) shouldBe true
      TestRoundedBigDecimalValue(2.0049) == TestRoundedBigDecimalValue(2.00) shouldBe true
    }

    "return false if compared with another instance of the same type representing different value" in {
      TestRoundedBigDecimalValue(2.01) == TestRoundedBigDecimalValue(2.00) shouldBe false
    }

    "return false if compared with different implementation of the same super type even when representing the same value" in {
      TestRoundedBigDecimalValue(2.005) == AnotherRoundedBigDecimalValue(2.005) shouldBe false
      TestRoundedBigDecimalValue(2.0049) == AnotherRoundedBigDecimalValue(2.049) shouldBe false
    }
  }
}
