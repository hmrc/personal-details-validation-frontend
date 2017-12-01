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

import cats.data.Validated
import cats.implicits._
import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.personaldetailsvalidationfrontend.model.JourneyId
import uk.gov.hmrc.personaldetailsvalidationfrontend.model.JourneyIdValueValidator.validate

package object binders {

  val bindingError: String = "binding-error: "

  implicit val journeyIdQueryBindable: QueryStringBindable[JourneyId] = new QueryStringBindable[JourneyId] {

    override def bind(key: String,
                      params: Map[String, Seq[String]]): Option[Either[String, JourneyId]] =
      params.get(key)
        .map(_.head)
        .map(validate)
        .map(toErrorMessageOrJourneyId)

    private val toErrorMessageOrJourneyId: Validated[IllegalArgumentException, String] => Either[String, JourneyId] = _.toEither.bimap(
      exception => bindingError + exception.getMessage,
      validated => JourneyId(validated)
    )

    override def unbind(key: String, value: JourneyId): String = value.toString()
  }
}
