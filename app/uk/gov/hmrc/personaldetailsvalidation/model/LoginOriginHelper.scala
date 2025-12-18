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

import play.api.mvc.Request

object LoginOriginHelper {

  def isDwp(implicit request: Request[?]): Boolean =
    request.session.get("origin").fold(false)(isDwp)

  def isDwp(loginOrigin: String): Boolean = loginOrigin.take(6).equalsIgnoreCase("dwp-iv")

  def isNotDwp(implicit request: Request[?]): Boolean = !isDwp

  def isSa(loginOrigin: String): Boolean = loginOrigin.endsWith("-sa")

  def isDwpOrSa(implicit request: Request[?]): Boolean =
    request.session.get("origin").fold(false)(isDwpOrSa)

  def isDwpOrSa(loginOrigin: String): Boolean = isDwp(loginOrigin) || isSa(loginOrigin)

  def isNotDwpOrSa(implicit request: Request[?]): Boolean = !isDwpOrSa

  def isDeskPro(loginOrigin : String) : Boolean =
    !(isDwp(loginOrigin) || Set("bta-sa", "pta-sa", "ssttp-sa")(loginOrigin))

}
