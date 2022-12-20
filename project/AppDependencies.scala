import sbt._

private object AppDependencies {

  def apply(): Seq[ModuleID] = compile ++ test

  private val compile = Seq(
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.12.2",
    "org.typelevel" %% "cats-core" % "2.0.0",
    "uk.gov.hmrc"   %% "bootstrap-frontend-play-28" % "5.24.0",
    "uk.gov.hmrc"   %% "domain" % "8.1.0-play-28",
    "uk.gov.hmrc"   %% "play-frontend-hmrc"  % "3.21.0-play-28",
    "uk.gov.hmrc"   %% "reactive-circuit-breaker"  % "3.5.0"
  )

  private val test = Seq(
    "org.jsoup" % "jsoup" % "1.13.1" % Test,
    "com.vladsch.flexmark" % "flexmark-all" % "0.35.10" % Test,
    "org.scalacheck" %% "scalacheck" % "1.15.3" % Test,
    "org.scalamock" %% "scalamock" % "5.1.0" % Test,
    "org.scalatest" %% "scalatest" % "3.2.3" % Test,
    "org.scalatestplus" %% "scalatestplus-mockito" % "1.0.0-M2" % Test,
    "org.scalatestplus" %% "scalatestplus-scalacheck" % "3.1.0.0-RC2" % Test,
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
    "org.pegdown" % "pegdown" % "1.6.0" % Test
  )

}