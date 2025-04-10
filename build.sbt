import play.sbt.PlayImport.PlayKeys.playDefaultPort
import play.sbt.routes.RoutesKeys
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "personal-details-validation-frontend"

ThisBuild / majorVersion := 1
ThisBuild / scalaVersion := "2.13.16"

lazy val playSettings: Seq[Setting[_]] = Seq(
  routesImport ++= Seq(
    "uk.gov.hmrc.personaldetailsvalidation.binders._",
    "uk.gov.hmrc.personaldetailsvalidation.model.CompletionUrl"
  )
)

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.components._",
)

lazy val scoverageSettings = {
  import scoverage._

  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;.*BuildInfo.*;.*views.*;.*Routes.*;.*RoutesPrefix.*;",
    ScoverageKeys.coverageMinimumStmtTotal := 65,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala, SbtDistributablesPlugin): _*)
  .settings(scalaSettings: _*)
  .settings(playSettings ++ scoverageSettings: _*)
  .settings(playDefaultPort := 9968)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(defaultSettings(): _*)
  .settings(
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
  .settings(resolvers ++= Seq(
    Resolver.jcenterRepo
  ))
  .settings(
    scalacOptions += s"-Wconf:src=${target.value}/.*:s"
  )
  .settings(A11yTest / unmanagedSourceDirectories += (baseDirectory.value / "accessibility"))
  .settings(
    RoutesKeys.routesImport += "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl"
  )

lazy val it = project
  .enablePlugins(PlayScala)
  .settings(majorVersion := 1)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.test)