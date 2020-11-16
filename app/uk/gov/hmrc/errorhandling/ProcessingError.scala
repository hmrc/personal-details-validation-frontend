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

package uk.gov.hmrc.errorhandling

sealed trait ProcessingError {
  def message: String

  override def toString: String = message

  def toQueryParam: Map[String, Seq[String]] = ???
}

case class TechnicalError(message: String) extends ProcessingError {
  override def toQueryParam: Map[String, Seq[String]] = Map("technicalError" -> Seq(""))
}

case class FailedDependencyError(message: String) extends ProcessingError {
  override def toQueryParam: Map[String, Seq[String]] = Map("deceased" -> Seq("true"))
}