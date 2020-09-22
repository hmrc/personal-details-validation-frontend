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

package uk.gov.voa.valuetype.tooling.generators

import org.scalacheck.{Arbitrary, Gen}

object GeneratorOf {

  def strings(minLength: Int = 1, maxLength: Int = 10) = {
    require(minLength <= maxLength, s"minLength $minLength cannot be greater than maxLength $maxLength")
    Gen.listOfN(Gen.chooseNum(minLength, maxLength).sample.get, Gen.alphaChar).map(_.mkString)
  }

  def nonEmptyStrings(maxLength: Int = 10) = strings(1, maxLength)

  val positiveInt = Gen.chooseNum(1, Int.MaxValue)

  val positiveLong = Gen.chooseNum(1L, Long.MaxValue)

  val positiveBigDecimal = Arbitrary.arbBigDecimal.arbitrary.retryUntil(_ > 0)
}
