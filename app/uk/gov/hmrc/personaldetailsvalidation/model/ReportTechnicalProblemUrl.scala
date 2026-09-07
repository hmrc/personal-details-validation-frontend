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

import play.api.mvc.Call

import java.net.URLEncoder

object ReportTechnicalProblemUrl {

  private val host = "www.tax.service.gov.uk"
  private val serviceNavParam = "useServiceNavigation"
  private val original: String = s"https://$host/contact/report-technical-problem?service=government-gateway-identity-verification-frontend"

  def apply(origin: String, call : Call) : String = {
    val url = if (LoginOriginHelper.isDeskPro(origin))
      original + s"&referrerUrl=${
        URLEncoder.encode(
          call.absoluteURL(true, host),
          "UTF-8"
        )
      }"
    else
      original

    s"$url&$serviceNavParam"
  }
}
