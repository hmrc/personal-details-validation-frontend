import sbt.*

 object AppDependencies {

  def apply(): Seq[ModuleID] = compile ++ test

  val bootstrapVersion = "10.7.0"

  val compile = Seq(
    "org.typelevel" %% "cats-core"                  % "2.13.0",
    "uk.gov.hmrc"   %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"   %% "domain-play-30"             % "13.0.0",
    "uk.gov.hmrc"   %% "play-frontend-hmrc-play-30" % "12.32.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.jsoup"               %  "jsoup"                  % "1.22.1",
    "uk.gov.hmrc"             %% "bootstrap-test-play-30" % bootstrapVersion,
    "org.scalacheck"          %% "scalacheck"             % "1.19.0",
    "org.scalamock"           %% "scalamock"              % "7.5.5",
    "org.scalatest"           %% "scalatest"              % "3.2.19",
    "org.scalatestplus"       %% "scalacheck-1-17"        % "3.2.18.0",
    "org.scalatestplus.play"  %% "scalatestplus-play"     % "7.0.2"
  ).map(_ % "test")

}