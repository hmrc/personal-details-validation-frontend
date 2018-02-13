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

package uk.gov.hmrc.http

import javax.inject.{Inject, Singleton}

import akka.stream.Materializer
import play.api.mvc.{Filter, RequestHeader, Result}
import uk.gov.hmrc.http.HeaderNames.googleAnalyticUserId

import scala.concurrent.Future

@Singleton
class AddGaUserIdInHeaderFilter @Inject()(implicit val mat: Materializer) extends Filter {

  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    val gaCookieValue = rh.cookies.get("_ga").map(_.value)
    lazy val gaUserIdInHeader = rh.headers.get(googleAnalyticUserId)

    val newHeaders = gaCookieValue.orElse(gaUserIdInHeader).foldLeft(rh.headers) { case (headers, userId) =>
      headers.replace(googleAnalyticUserId -> userId)
    }
    f(rh.copy(headers = newHeaders))
  }
}