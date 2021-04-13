import play.sbt.PlayImport._
import sbt._

private object AppDependencies {

  def apply(): Seq[ModuleID] = compile ++ test ++ it

  private val compile = Seq(
    "org.typelevel" %% "cats-core" % "1.0.0-RC1",
    "uk.gov.hmrc" %% "bootstrap-frontend-play-26" % "3.0.0",
    "uk.gov.hmrc" %% "domain" % "5.10.0-play-26",
    "uk.gov.hmrc" %% "govuk-template" % "5.56.0-play-26",
    "uk.gov.hmrc" %% "play-language" % "4.4.0-play-26",
    "uk.gov.hmrc" %% "play-ui" % "9.0.0-play-26",
    "uk.gov.hmrc" %% "play-frontend-govuk" % "0.60.0-play-26",
    "uk.gov.hmrc" %% "play-frontend-hmrc" % "0.38.0-play-26",
    ws
  )

  private val test = Seq(
    "org.jsoup" % "jsoup" % "1.10.2" % Test,
    "org.scalacheck" %% "scalacheck" % "1.13.4" % Test,
    "org.scalamock" %% "scalamock" % "4.1.0" % Test,
    "org.mockito" % "mockito-core" % "3.8.0" % Test,
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
    "uk.gov.hmrc" %% "service-integration-test" % "0.12.0-play-26" % Test,
    "org.pegdown" % "pegdown" % "1.6.0" % Test

  )

  private val it = Seq(
    "com.github.tomakehurst" % "wiremock-jre8" % "2.27.2" % IntegrationTest,
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % IntegrationTest,
    "uk.gov.hmrc" %% "service-integration-test" % "0.12.0-play-26" % IntegrationTest,
    "org.pegdown" % "pegdown" % "1.6.0" % IntegrationTest
  )
}
