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

package uk.gov.hmrc.personaldetailsvalidation

import java.net.URLEncoder

import cats.implicits._
import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.personaldetailsvalidation.model.CompletionUrl
import uk.gov.hmrc.personaldetailsvalidation.model.CompletionUrl.completionUrl

package object binders {

  implicit val completionUrlQueryBinder: QueryStringBindable[CompletionUrl] = new QueryStringBindable[CompletionUrl] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, CompletionUrl]] =
      getValue(key, params).map { value =>
        completionUrl(value).leftMap(_.getMessage)
      }

    override def unbind(key: String, value: CompletionUrl): String = s"$key=${URLEncoder.encode(value.toString(), "UTF-8")}"
  }

  private def getValue(key: String, params: Map[String, Seq[String]]): Option[String] = params.get(key).map(_.head)

}
