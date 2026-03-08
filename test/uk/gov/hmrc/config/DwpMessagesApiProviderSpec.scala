/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.config

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, MessagesApi}
import support.UnitSpec

class DwpMessagesApiProviderSpec
  extends UnitSpec
    with GuiceOneAppPerSuite {

  "DwpMessagesApiProvider" should {

    "provide a MessagesApi that can resolve standard messages" in {
      val provider = app.injector.instanceOf[DwpMessagesApiProvider]
      val messagesApi: MessagesApi = provider.get

      val msgEn = messagesApi("error.prefix")(Lang("en"))

      msgEn.nonEmpty shouldBe true
    }

    "provide a MessagesApi that can resolve Welsh messages" in {
      val provider    = app.injector.instanceOf[DwpMessagesApiProvider]
      val messagesApi = provider.get

      val msgCy = messagesApi("error.prefix")(Lang("cy"))

      msgCy.nonEmpty shouldBe true
    }

    "provide a MessagesApi where the default lang messages are non-empty" in {
      val provider    = app.injector.instanceOf[DwpMessagesApiProvider]
      val messagesApi = provider.get

      messagesApi.messages.get("default") shouldBe defined
      messagesApi.messages("default") should not be empty
    }

    "provide a MessagesApi where the default.play lang messages are non-empty" in {
      val provider    = app.injector.instanceOf[DwpMessagesApiProvider]
      val messagesApi = provider.get

      messagesApi.messages.get("default.play") shouldBe defined
    }

    "resolve the service name message key" in {
      val provider    = app.injector.instanceOf[DwpMessagesApiProvider]
      val messagesApi = provider.get

      val serviceName = messagesApi("service.name")(Lang("en"))

      serviceName.nonEmpty shouldBe true
    }
  }
}
