/*
 * Copyright 2018 HM Revenue & Customs
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

import org.scalamock.scalatest.MockFactory
import org.slf4j
import play.api.LoggerLike
import uk.gov.hmrc.errorhandling.ProcessingError
import uk.gov.hmrc.play.test.UnitSpec

class LoggerSpec
  extends UnitSpec
    with MockFactory {

  "error" should {

    "delegate to the given logger" in new Setup {
      val error = ProcessingError("message")

      underlyingLogger.expect.error("message")

      logger.error(error)
    }
  }

  private trait Setup {

    val underlyingLogger = new LoggerLike {

      override val logger: slf4j.Logger = mock[slf4j.Logger]

      def expect = new {
        def error(message: String) = {
          (logger.isErrorEnabled: () => Boolean)
            .expects()
            .returning(true)

          (logger.error(_: String))
            .expects(message)
        }
      }
    }

    val logger = new Logger(underlyingLogger)
  }
}
