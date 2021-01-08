/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.logging

import org.scalamock.scalatest.MixedMockFactory
import play.api.LoggerLike
import uk.gov.hmrc.errorhandling.ProcessingError
import support.UnitSpec

class LoggerSpec
  extends UnitSpec
    with MixedMockFactory {

  "error" should {

    "delegate to the given logger" in new Setup {
      val error = ProcessingError("message")

      underlyingLogger.expects('error)(argAssert {
        (message: () => String) =>
          message() shouldBe "message"
      }, *)

      logger.error(error)
    }
  }

  private trait Setup {

    val underlyingLogger = Proxy.mock[LoggerLike]

    val logger = new Logger(underlyingLogger)
  }
}
