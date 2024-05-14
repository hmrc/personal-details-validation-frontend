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

package uk.gov.hmrc.language

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.MessagesApi
import support.UnitSpec

class MessagesEntriesSpec extends UnitSpec with GuiceOneAppPerSuite {

  private val keysForPendingTranslations = Set(
    "global.error.InternalServerError500.message",
    "global.error.InternalServerError500.title",
    "global.error.InternalServerError500.heading",
    "service_temporarily_unavailable.p2.sa",
    "service_temporarily_unavailable.h1",
    "service_temporarily_unavailable.p2",
    "service_temporarily_unavailable.p2.dwp",
    "service_temporarily_unavailable.p1"
  )
  private val keysNotNeeded = Set(
    "global.error.badRequest400.title",
    "global.error.badRequest400.heading",
    "global.error.badRequest400.message",
    "global.error.pageNotFound404.message",
    "global.error.pageNotFound404.title",
    "global.error.pageNotFound404.heading",
    "footer.links.privacy_policy.url",
    "footer.links.terms_and_conditions.url",
    "footer.links.help_page.url",
    "footer.links.cookies.url",
    "pertax.attorney.banner.user",
    "pertax.attorney.banner.link",
    "error.enter_valid_date",
    "error.enter_a_date",
    "error.positive.number",
    "error.enter_numbers_",
    "error.postcode.length.violation",
    "error.address.invalid.character",
    "error.address.blank",
    "error.postcode.invalid.character",
    "error.email",
    "error.address.main.line.max.length.violation",
    "error.address.optional.line.max.length.violation",
    "common.firstlogin",
    "attorney.banner.nan",
    "attorney.banner.user",
    "attorney.banner.link",
    "common.previousLoginTime",
    "common.signOut",
    "label.beta",
    "footer.welshHelp.text",
    "footer.contact.url",
    "footer.welshHelp.url",
    "footer.govukHelp.url",
    "footer.privacy.url",
    "footer.termsConditions.url",
    "footer.cookies.url"
  )
  private val excludedKeysSet: Set[String] =
    keysForPendingTranslations ++ keysNotNeeded

  private val excludedKeys: ((String, String)) => Boolean = {
    case (k, _) => excludedKeysSet.contains(k)
  }

  private val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  private val defaultMessages: Map[String, String] = messagesApi.messages("default").filterNot(excludedKeys)
  private val cyMessages: Map[String, String] = messagesApi.messages("cy").filterNot(excludedKeys)

  "There should be the same messages keys defined in both 'default' and 'cy'" in {
    defaultMessages.keySet diff cyMessages.keySet shouldBe empty
  }

  "There should be non empty messages defined for all keys in both 'default' and 'cy'" in {
    defaultMessages filter emptyValues shouldBe empty
    cyMessages filter emptyValues shouldBe empty
  }

  private val emptyValues: ((String, String)) => Boolean = {
    case (_, v) => v.isEmpty
  }

}
