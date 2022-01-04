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

package uk.gov.hmrc.personaldetailsvalidation.model

import play.api.data.{Form, FormError}

//VER-1008
object DateErrorMessage {

  def dateErrors(form: Form[_], fieldName: String): Seq[FormError] = form(fieldName).errors

  def getErrorMessage(form: Form[_], fieldName: String): String = {
    if (dateErrors(form, fieldName).length > 1 && form(fieldName).errors.map(_.message.contains("required")).length > 1) {
      dateMessageMaker(
        form(fieldName).errors.map(f => f.message).exists(_.contains("day.required")),
        form(fieldName).errors.map(f => f.message).exists(_.contains("month.required")),
        form(fieldName).errors.map(f => f.message).exists(_.contains("year.required")),
        fieldName
      )
    }
    else {
      form(fieldName).error.map(_.message).getOrElse("")
    }
  }

  def getErrorSummaryMessage(fieldName: String, messages: Seq[String]): List[String] = {
    if (messages.length > 1 && messages.map(_.contains("required")).length > 1){
      List(
        dateMessageMaker(
          messages.map(m => m).exists(_.contains("day.required")),
          messages.map(m => m).exists(_.contains("month.required")),
          messages.map(m => m).exists(_.contains("year.required")),
          fieldName
        )
      )
    }
    else messages.toList
  }

  private def dateMessageMaker(argOne: Boolean, argTwo: Boolean, argThr: Boolean, fieldName: String): String = {
    (argOne, argTwo, argThr) match {
      case (true, false, false) => s"personal-details.$fieldName.miss.day"
      case (true, true, false) => s"personal-details.$fieldName.miss.day.month"
      case (true, false, true) => s"personal-details.$fieldName.miss.day.year"
      case (false, true, false) => s"personal-details.$fieldName.miss.month"
      case (false, true, true) => s"personal-details.$fieldName.miss.month.year"
      case (false, false, true) => s"personal-details.$fieldName.miss.year"
      case _ => s"personal-details.$fieldName.required"
    }
  }

}
