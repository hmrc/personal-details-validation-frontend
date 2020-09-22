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

case class TestStringValue(value: String) extends StringValue

case class TestIntValue(value: Int) extends IntValue

case class TestLongValue(value: Long) extends LongValue

case class TestBooleanValue(value: Boolean) extends BooleanValue

case class TestBigDecimalValue(value: BigDecimal) extends BigDecimalValue

case class TestRoundedBigDecimalValue(nonRoundedValue: BigDecimal) extends RoundedBigDecimalValue {

  protected[this] def isOfThisInstance(other: RoundedBigDecimalValue) =
    other.isInstanceOf[TestRoundedBigDecimalValue]

}
