/*
 * Copyright 2020 HM Revenue & Customs
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

import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.HttpConfiguration
import play.api.i18n.DefaultLangsProvider
import play.api.{ConfigLoader, Configuration, Environment}
import uk.gov.hmrc.config.DwpMessagesApiProvider
import uk.gov.hmrc.views.ViewConfig

import scala.collection.JavaConverters._

object ViewConfigMockFactory extends MockFactory with GuiceOneAppPerSuite {

  private def configuration: Configuration = {

    val configMock = mock[Configuration]

    (configMock.getString _).expects("assets.url", *).returning(Some("assets-url"))
    (configMock.getString _).expects("assets.version", *).returning(Some("assets-version"))
    (configMock.getString _).expects("optimizely.url", *).returning(None)
    (configMock.getString _).expects("optimizely.projectId", *).returning(None)
    (configMock.getString _).expects("google-analytics.token", *).returning(Some("ga-token"))
    (configMock.getString _).expects("google-analytics.host", *).returning(Some("ga-host"))
    (configMock.getStringList _).expects("play.i18n.langs").anyNumberOfTimes().returning(Some(List("en", "cy").asJava))
    (configMock.getString _).expects("play.i18n.descriptions.en", *).anyNumberOfTimes().returning(Some("english"))
    (configMock.getString _).expects("play.i18n.descriptions.en", None).anyNumberOfTimes().returning(Some("english"))
    (configMock.getString _).expects("play.i18n.descriptions.cy", *).anyNumberOfTimes().returning(Some("cymraeg"))
    (configMock.getString _).expects("play.i18n.descriptions.cy", None).anyNumberOfTimes().returning(Some("cymraeg"))
    (configMock.getString _).expects("dwp.originLabel", *).returning(Some("dwp-iv"))
    (configMock.getOptional(_: String)(_:ConfigLoader[String])).expects("dwp.originLabel", *).returning(Some("dwp-iv"))
    (configMock.getString _).expects("dwp.getHelpUrl", *).returning(Some("someGetHelpUrl"))

    configMock
  }

  private def messagesApi: DwpMessagesApiProvider = {

    val dwpMessagesApi = new DwpMessagesApiProvider(
      environment = Environment.simple(),
      configuration = app.configuration,
      langs = new DefaultLangsProvider(app.configuration).get,
      HttpConfiguration()
    )

    dwpMessagesApi
  }

  def apply(): ViewConfig =
    new ViewConfig(configuration, messagesApi)
}
