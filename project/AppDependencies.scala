import play.sbt.PlayImport._
import sbt._

private object AppDependencies {

  def apply(): Seq[ModuleID] = compile ++ test()

  private val compile = Seq(
    "org.typelevel" %% "cats-core" % "1.0.0-RC1",
    "uk.gov.hmrc" %% "bootstrap-play-25" % "1.0.0",
    "uk.gov.hmrc" %% "govuk-template" % "5.15.0" withSources(),
    "uk.gov.hmrc" %% "play-ui" % "7.9.0" withSources(),
    "uk.gov.hmrc" %% "valuetype" % "1.1.0",
    ws
  )

  private def test(scope: String = "test") = Seq(
    "org.jsoup" % "jsoup" % "1.10.2" % scope,
    "org.scalacheck" %% "scalacheck" % "1.13.4" % scope,
    "org.scalamock" %% "scalamock" % "4.0.0" % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % scope,
    "uk.gov.hmrc" %% "hmrctest" % "3.0.0" % scope
  )
}
