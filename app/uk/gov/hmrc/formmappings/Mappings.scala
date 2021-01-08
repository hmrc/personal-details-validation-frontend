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

package uk.gov.hmrc.formmappings

import java.time.LocalDate

import play.api.data.Forms._
import play.api.data.Mapping
import uk.gov.hmrc.personaldetailsvalidation.model.NonEmptyString

object Mappings {

  def mandatoryText(error: => String): Mapping[NonEmptyString] =
    optional(text)
      .transform[Option[String]](emptyStringAsNone, emptyStringAsNone)
      .verifying(error, _.isDefined)
      .transform[NonEmptyString](validatedNonBlankString => validatedNonBlankString.map(NonEmptyString.apply).get, nonEmptyString => Some(nonEmptyString.value))

  val optionalText: Mapping[Option[NonEmptyString]] =
    optional(text)
      .transform[Option[String]](emptyStringAsNone, emptyStringAsNone)
      .transform[Option[NonEmptyString]](validatedNonBlankString => validatedNonBlankString.map(NonEmptyString.apply), nonEmptyString => nonEmptyString.map(_.value))

  private def emptyStringAsNone(maybeValue: Option[String]): Option[String] = maybeValue.map(_.trim).flatMap {
    case "" => Option.empty[String]
    case nonEmpty => Some(nonEmpty)
  }

  def mandatoryLocalDate(errorKeyPrefix: => String): Mapping[LocalDate] =
    LocalDateMapping()(errorKeyPrefix)
}

