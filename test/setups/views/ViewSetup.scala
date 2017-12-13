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

package setups.views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalamock.scalatest.MockFactory
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.personaldetailsvalidation.views.ViewConfig

import scala.language.implicitConversions

abstract class ViewSetup(implicit app: Application) extends MockFactory {
  implicit val viewConfig: ViewConfig = ViewConfigMockFactory()
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  implicit val request: Request[AnyContentAsEmpty.type] = FakeRequest()
  implicit val lang: Lang = Lang("en")
  implicit val messages: Messages = Messages.Implicits.applicationMessages

  implicit def asDocument(html: Html): Document = Jsoup.parse(html.toString())
}