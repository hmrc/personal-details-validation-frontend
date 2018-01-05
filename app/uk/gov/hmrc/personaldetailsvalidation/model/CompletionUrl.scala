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

package uk.gov.hmrc.personaldetailsvalidation.model

import cats.implicits._
import uk.gov.voa.valuetype.StringValue

case class CompletionUrl private[CompletionUrl](value: String) extends StringValue

object CompletionUrl {

  def completionUrl(value: String): Either[IllegalArgumentException, CompletionUrl] = for {
    _ <- validateRelativeUrl(value)
    _ <- validateProtocolRelativeUrlSafe(value)
  } yield CompletionUrl(value)

  private def validateRelativeUrl(value: String) = validate(value.startsWith("/"), s"$value is not a relative url")

  private def validateProtocolRelativeUrlSafe(value: String) = validate(!value.contains("//"), s"$value is not protocol relative url safe")

  private def validate(condition: => Boolean, errorMessage: => String): Either[IllegalArgumentException, Unit] =
    Either.cond(condition, (), new IllegalArgumentException(errorMessage))

}
