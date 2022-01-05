import sbt._

private object AppDependencies {

  def apply(): Seq[ModuleID] = compile ++ test

  private val compile = Seq(
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.12.2",
    "org.typelevel" %% "cats-core" % "2.0.0",
    "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % "5.16.0",
    "uk.gov.hmrc" %% "domain" % "6.0.0-play-28",
    "uk.gov.hmrc" %% "play-language" % "5.1.0-play-28",
    "uk.gov.hmrc" %% "play-frontend-hmrc"  % "1.26.0-play-28"
  )

  private val test = Seq(
    "org.jsoup" % "jsoup" % "1.13.1" % Test,
    "org.scalacheck" %% "scalacheck" % "1.15.3" % Test,
    "org.scalamock" %% "scalamock" % "5.1.0" % Test,
    "org.mockito" % "mockito-core" % "3.8.0" % Test,
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test,
    "uk.gov.hmrc" %% "service-integration-test" % "1.1.0-play-27" % Test,
    "org.pegdown" % "pegdown" % "1.6.0" % Test
  )

}
