import TestPhases.oneForkedJvmPerTest
import play.sbt.PlayImport.PlayKeys.playDefaultPort
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "personal-details-validation-frontend"

lazy val playSettings: Seq[Setting[_]] = Seq(
  routesImport ++= Seq(
    "uk.gov.hmrc.personaldetailsvalidation.binders._",
    "uk.gov.hmrc.personaldetailsvalidation.model.CompletionUrl"
  )
)
lazy val silencerVersion = "1.7.0"

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.govukfrontend.views.html.helpers._",
  "uk.gov.hmrc.hmrcfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)

lazy val scoverageSettings = {
  import scoverage._

  Seq(
    ScoverageKeys.coverageExcludedPackages :=
      """<empty>;
        |Reverse.*;
        |.*BuildInfo.*;
        |.*views.*;
        |.*Routes.*;
        |.*RoutesPrefix.*;""".stripMargin,
    ScoverageKeys.coverageMinimum := 80,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory): _*)
  .settings(majorVersion := 1)
  .settings(scalaSettings: _*)
  .settings(playSettings ++ scoverageSettings: _*)
  .settings(publishingSettings: _*)
  .settings(playDefaultPort := 9968)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(defaultSettings(): _*)
  .settings(
    scalaVersion := "2.12.12",
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true
  )
  .settings(
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-language:implicitConversions",
      "-language:reflectiveCalls",
      "-language:postfixOps",
      "-language:higherKinds"
    )
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    Keys.fork in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest) (base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    parallelExecution in IntegrationTest := false
  )
  .settings(resolvers ++= Seq(
    Resolver.bintrayRepo("hmrc", "releases"),
    Resolver.jcenterRepo
  ))

  // this should be removed once we are able to use Scala 2.12.14 (or 2.13)
  .settings(
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    )
  )
  .settings(
    // silence all warnings on autogenerated files
    scalacOptions += "-P:silencer:pathFilters=target/.*",
    // scalacOptions += s"-Wconf:src=${target.value}/.*:s"  // Scala 2.12.14 equivalent
  )