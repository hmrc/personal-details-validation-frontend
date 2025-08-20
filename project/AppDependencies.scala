import sbt._

 object AppDependencies {

  def apply(): Seq[ModuleID] = compile ++ test

  val bootstrapVersion = "10.1.0"

  val compile = Seq(
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.19.2",
    "org.typelevel" %% "cats-core" % "2.13.0",
    "uk.gov.hmrc"   %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"   %% "domain-play-30" % "11.0.0",
    "uk.gov.hmrc"   %% "play-frontend-hmrc-play-30"  % "12.8.0",
    "uk.gov.hmrc"   %% "reactive-circuit-breaker"  % "5.0.0"
  )

  val test = Seq(
    "org.jsoup" % "jsoup" % "1.21.1" % Test,
    "com.vladsch.flexmark" % "flexmark-all" % "0.64.8" % Test,
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVersion % Test,
    "org.scalacheck" %% "scalacheck" % "1.18.1" % Test,
    "org.scalamock" %% "scalamock" % "7.4.1" % Test,
    "org.scalatest" %% "scalatest" % "3.2.19" % Test,
    "org.scalatestplus" %% "scalatestplus-mockito" % "1.0.0-M2" % Test,
    "org.scalatestplus" %% "scalacheck-1-17" % "3.2.18.0" % Test,
    "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test
  )

}