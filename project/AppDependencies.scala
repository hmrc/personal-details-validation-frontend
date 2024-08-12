import sbt._

private object AppDependencies {

  def apply(): Seq[ModuleID] = compile ++ test

  val bootstrapVersion = "8.6.0"

  private val compile = Seq(
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.17.0",
    "org.typelevel" %% "cats-core" % "2.10.0",
    "uk.gov.hmrc"   %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"   %% "domain-play-30" % "9.0.0",
    "uk.gov.hmrc"   %% "play-frontend-hmrc-play-30"  % "10.6.0",
    "uk.gov.hmrc"   %% "reactive-circuit-breaker"  % "5.0.0"
  )

  private val test = Seq(
    "org.jsoup" % "jsoup" % "1.17.2" % Test,
    "com.vladsch.flexmark" % "flexmark-all" % "0.64.8" % Test,
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVersion % Test,
    "org.scalacheck" %% "scalacheck" % "1.18.0" % Test,
    "org.scalamock" %% "scalamock" % "6.0.0" % Test,
    "org.scalatest" %% "scalatest" % "3.2.18" % Test,
    "org.scalatestplus" %% "scalatestplus-mockito" % "1.0.0-M2" % Test,
    "org.scalatestplus" %% "scalacheck-1-17" % "3.2.18.0" % Test,
    "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
    "org.pegdown" % "pegdown" % "1.6.0" % Test
  )

}