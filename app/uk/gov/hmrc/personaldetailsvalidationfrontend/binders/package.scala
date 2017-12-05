/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.personaldetailsvalidationfrontend

import java.util.UUID

import cats.data.Validated
import cats.implicits._
import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.personaldetailsvalidationfrontend.model.JourneyId

package object binders {

  val bindingError: String = "binding-error: "

  implicit val journeyIdQueryBindable: QueryStringBindable[JourneyId] = new QueryStringBindable[JourneyId] {

    override def bind(key: String,
                      params: Map[String, Seq[String]]): Option[Either[String, JourneyId]] =
      params.get(key)
        .map(_.head)
        .map(toValidated)
        .map(toErrorMessageOrJourneyId)

    private def toValidated(v: String): Validated[IllegalArgumentException, JourneyId] =
      Validated.catchOnly[IllegalArgumentException] {
        JourneyId(UUID.fromString(v))
      }

    private def toErrorMessageOrJourneyId(validated: Validated[IllegalArgumentException, JourneyId]): Either[String, JourneyId] =
      validated.toEither.bimap(
        exception => bindingError + exception.getMessage,
        identity
      )

    override def unbind(key: String, value: JourneyId): String = value.toString()
  }
}