/*
 * Copyright 2022 HM Revenue & Customs
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

import javax.inject.Singleton

import play.api.{LoggerLike, Logger => PlayLogger}
import uk.gov.hmrc.errorhandling.ProcessingError

@Singleton
class Logger(logger: LoggerLike) {

  def this() = this(PlayLogger("application"))

  def error(error: ProcessingError): Unit = logger.error(error.message)
}
