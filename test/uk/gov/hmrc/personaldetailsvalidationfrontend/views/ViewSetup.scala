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

package uk.gov.hmrc.personaldetailsvalidationfrontend.views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalamock.scalatest.MockFactory
import play.api.i18n.{Lang, Messages}
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.{Application, Configuration}
import play.twirl.api.Html
import uk.gov.hmrc.personaldetailsvalidationfrontend.config.ViewConfig

import scala.language.implicitConversions

abstract class ViewSetup(implicit app: Application) extends MockFactory {
  implicit val viewConfig: ViewConfig = ViewConfigMockFactory()

  implicit val request: Request[AnyContentAsEmpty.type] = FakeRequest()
  implicit val lang: Lang = Lang("en")
  implicit val messages: Messages = Messages.Implicits.applicationMessages

  implicit def asDocument(html: Html): Document = Jsoup.parse(html.toString())
}

private object ViewConfigMockFactory extends MockFactory {

  private def configuration: Configuration = {
    val configMock = mock[Configuration]

    (configMock.getString _).expects("assets.url", *).returning(Some("assets-url"))
    (configMock.getString _).expects("assets.version", *).returning(Some("assets-version"))
    (configMock.getString _).expects("optimizely.url", *).returning(None)
    (configMock.getString _).expects("optimizely.projectId", *).returning(None)
    (configMock.getString _).expects("google-analytics.token", *).returning(Some("ga-token"))
    (configMock.getString _).expects("google-analytics.host", *).returning(Some("ga-host"))
    (configMock.getString _).expects("microservice.services.protocol", *).returning(Some("http"))
    (configMock.getString _).expects("microservice.services.contact-frontend.protocol", *).returning(Some("http"))
    (configMock.getString _).expects("microservice.services.contact-frontend.host", *).returning(Some("contant-frontend-host"))
    (configMock.getInt _).expects("microservice.services.contact-frontend.port").returning(Some(4444))

    configMock
  }

  def apply(): ViewConfig = {
    new ViewConfig(configuration)
  }
}