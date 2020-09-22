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

sealed trait TestStringOption extends StringValue

object TestStringOption extends StringOptions[TestStringOption] {

  case object TestOption1 extends TestStringOption {
    val value = "1"
  }

  case object TestOption2 extends TestStringOption {
    val value = "2"
  }

  val all = Seq(
    TestOption1,
    TestOption2
  )
}


sealed trait TestIntOption extends IntValue

object TestIntOption extends IntOptions[TestIntOption] {

  case object TestOption5 extends TestIntOption {
    val value = 5
  }

  case object TestOption6 extends TestIntOption {
    val value = 6
  }

  case object TestOption7 extends TestIntOption {
    val value = 7
  }

  val all = Seq(
    TestOption5,
    TestOption6,
    TestOption7
  )
}


sealed trait TestLongOption extends LongValue

object TestLongOption extends LongOptions[TestLongOption] {

  case object TestOption5 extends TestLongOption {
    val value = 5L
  }

  case object TestOption6 extends TestLongOption {
    val value = 6L
  }

  case object TestOption7 extends TestLongOption {
    val value = 7L
  }

  val all = Seq(
    TestOption5,
    TestOption6,
    TestOption7
  )
}
