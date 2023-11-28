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

import play.api.data.Forms.{of, single}
import play.api.data.format.Formatter
import play.api.data.{Form, FormError}
import play.api.i18n.Messages

sealed trait UserHasNino

case object UserHasNinoTrue extends UserHasNino
case object UserHasNinoFalse extends UserHasNino

object DoYouHaveYourNino {
  implicit def formatter(implicit messages: Messages): Formatter[UserHasNino] = new Formatter[UserHasNino] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], UserHasNino] = {
      data.get(key) match {
        case Some("yes") => Right(UserHasNinoTrue)
        case Some("no") => Right(UserHasNinoFalse)
        case _ => Left(Seq(FormError(key, messages("do_you_have_your_nino.error"))))
      }
    }

    override def unbind(key: String, value: UserHasNino) = Map()
  }

  def apply()(implicit messages: Messages): Form[UserHasNino] = Form(
    single("do_you_have_your_nino" -> of[UserHasNino])
  )

}

