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

package setups.views

import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.HttpConfiguration
import play.api.i18n.DefaultLangsProvider
import play.api.{ConfigLoader, Configuration, Environment}
import uk.gov.hmrc.config.DwpMessagesApiProvider
import uk.gov.hmrc.views.ViewConfig

object ViewConfigMockFactory extends MockFactory with GuiceOneAppPerSuite {

  private def configuration: Configuration = {

    val configMock = mock[Configuration]

    (configMock.get[String](_ : String)(_ : ConfigLoader[String])).expects("assets.url", *).returning("assets-url")
    (configMock.get[String](_ : String)(_ : ConfigLoader[String])).expects("assets.version", *).returning("assets-version")
    (configMock.get[String](_ : String)(_ : ConfigLoader[String])).expects("optimizely.url", *).returning(null)
    (configMock.get[String](_ : String)(_ : ConfigLoader[String])).expects("optimizely.projectId", *).returning(null)
    (configMock.get[String](_ : String)(_ : ConfigLoader[String])).expects("google-analytics.token", *).returning("ga-token")
    (configMock.get[String](_ : String)(_ : ConfigLoader[String])).expects("google-analytics.host", *).returning("ga-host")
    (configMock.getOptional[Seq[String]](_ : String)(_ : ConfigLoader[Seq[String]])).expects("play.i18n.langs", *).anyNumberOfTimes().returning(Some(Seq("en", "cy")))
    (configMock.get[String](_ : String)(_ : ConfigLoader[String])).expects("play.i18n.descriptions.en", *).anyNumberOfTimes().returning("english")
    (configMock.get[String](_ : String)(_ : ConfigLoader[String])).expects("play.i18n.descriptions.en", *).anyNumberOfTimes().returning("english")
    (configMock.get[String](_ : String)(_ : ConfigLoader[String])).expects("play.i18n.descriptions.cy", *).anyNumberOfTimes().returning("cymraeg")
    (configMock.get[String](_ : String)(_ : ConfigLoader[String])).expects("play.i18n.descriptions.cy", *).anyNumberOfTimes().returning("cymraeg")
    (configMock.get[String](_ : String)(_ : ConfigLoader[String])).expects("dwp.originLabel", *).returning("dwp-iv")
    (configMock.getOptional(_: String)(_:ConfigLoader[String])).expects("dwp.originLabel", *).returning(Some("dwp-iv"))
    (configMock.get[String](_ : String)(_ : ConfigLoader[String])).expects("dwp.getHelpUrl", *).returning("someGetHelpUrl")

    (configMock.get(_: String)(_:ConfigLoader[Int])).expects(v1 = "timeoutDialog.timeout-seconds", *).returning(5)
    (configMock.get(_: String)(_:ConfigLoader[Int])).expects(v1 = "timeoutDialog.timeout-countdown-seconds", *).returning(5)

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
