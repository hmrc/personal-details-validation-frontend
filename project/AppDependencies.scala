import play.sbt.PlayImport._
import sbt._

private object AppDependencies {

  def apply(): Seq[ModuleID] = compile ++ test ++ it

  private val compile = Seq(
    "org.typelevel" %% "cats-core" % "1.0.0-RC1",
    "uk.gov.hmrc" %% "bootstrap-play-25" % "4.12.0",
    "uk.gov.hmrc" %% "domain" % "5.10.0-play-25",
    "uk.gov.hmrc" %% "govuk-template" % "5.56.0-play-25",
    "uk.gov.hmrc" %% "play-language" % "3.4.0",
    "uk.gov.hmrc" %% "play-ui" % "8.12.0-play-25",
    "uk.gov.hmrc" %% "valuetype" % "1.1.0",
    ws
  )

  private val test = Seq(
    "org.jsoup" % "jsoup" % "1.10.2" % Test,
    "org.scalacheck" %% "scalacheck" % "1.13.4" % Test,
    "org.scalamock" %% "scalamock" % "4.0.0" % Test,
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % Test,
    "uk.gov.hmrc" %% "service-integration-test" % "0.12.0-play-25" % Test,
    "org.pegdown" % "pegdown" % "1.6.0" % Test

  )

  private val it = Seq(
    "com.github.tomakehurst" % "wiremock" % "2.8.0" % IntegrationTest,
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % IntegrationTest,
    "uk.gov.hmrc" %% "service-integration-test" % "0.9.0-play-25" % IntegrationTest,
    "org.pegdown" % "pegdown" % "1.6.0" % IntegrationTest
  )
}
