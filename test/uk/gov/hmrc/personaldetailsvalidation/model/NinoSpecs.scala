/*
 * Copyright 2023 HM Revenue & Customs
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

import support.UnitSpec
import uk.gov.hmrc.domain.Nino

import scala.util.Try

class NinoSpecs extends UnitSpec {

  "Nino validation should work as expected" should {
    "validate nino with spaces" in {
      Try(Nino(("AA 00 00 03 D").replace(" ", "").toUpperCase)).isSuccess shouldBe true // 4 spaces
      Try(Nino(("XX 000 000 D").replace(" ", "").toUpperCase)).isSuccess shouldBe true // three spaces
      Try(Nino(("AA 0000 03 D").replace(" ", "").toUpperCase)).isSuccess shouldBe true // three spaces variation
      Try(Nino(("AA  0000 03 D").replace(" ", "").toUpperCase)).isSuccess shouldBe true // with a double space
    }

    "validate nino incorrect format" in {
      Try(Nino(("ddd ddd").replace(" ", "").toUpperCase)).isSuccess shouldBe false // not a nino format
      Try(Nino(("333 3333 333").replace(" ", "").toUpperCase)).isSuccess shouldBe false // not a nino format
    }
  }
}
