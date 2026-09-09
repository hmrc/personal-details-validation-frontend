import sbt.*

 object AppDependencies {

  def apply(): Seq[ModuleID] = compile ++ test

  val bootstrapVersion = "10.8.0"

  val compile = Seq(
    "org.typelevel" %% "cats-core"                  % "2.13.0",
    "uk.gov.hmrc"   %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"   %% "domain-play-30"             % "13.0.0",
    "uk.gov.hmrc"   %% "play-frontend-hmrc-play-30" % "13.13.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.jsoup"               %  "jsoup"                  % "1.23.2",
    "uk.gov.hmrc"             %% "bootstrap-test-play-30" % bootstrapVersion,
    "org.scalacheck"          %% "scalacheck"             % "1.20.0",
    "org.scalamock"           %% "scalamock"              % "7.5.5",
    "org.scalatest"           %% "scalatest"              % "3.2.20",
    "org.scalatestplus"       %% "scalacheck-1-17"        % "3.2.18.0",
    "org.scalatestplus.play"  %% "scalatestplus-play"     % "7.0.2",
    "uk.gov.hmrc"             %% "domain-test-play-30"    % "13.0.0"
  ).map(_ % "test")

}