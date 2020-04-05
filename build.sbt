import xerial.sbt.Sonatype._

ThisBuild / name := "mtg4s"
ThisBuild / scalaVersion := "2.13.1"
ThisBuild / organization := "com.gaborpihaj"
ThisBuild / dynverSonatypeSnapshots := true
ThisBuild / scalafixDependencies += "com.nequissimus" %% "sort-imports" % "0.3.2"

ThisBuild / publishTo := sonatypePublishToBundle.value

val catsVersion = "2.1.1"
val catsEffectVersion = "2.1.2"
val enumeratumVersion = "1.5.13"
val enumeratumCirceVersion = "1.5.23"
val circeVersion = "0.12.3"
val scalatestVersion = "3.1.1"

lazy val publishSettings = List(
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  publishMavenStyle := true,
  sonatypeProjectHosting := Some(GitHubHosting("voidcontext", "mtg4s", "gabor.pihaj@gmail.com"))
)

lazy val defaultSettings = Seq(
  addCompilerPlugin(scalafixSemanticdb)
)

lazy val mtgjson = (project in file("mtgjson"))
  .settings(publishSettings)
  .settings(
    defaultSettings,
    parallelExecution in Test := false,
    libraryDependencies ++= Seq(
      "org.typelevel"  %% "cats-core"        % catsVersion,
      "org.typelevel"  %% "cats-effect"      % catsEffectVersion,
      "com.beachape"   %% "enumeratum"       % enumeratumVersion,
      "io.circe"       %% "circe-core"       % circeVersion,
      "io.circe"       %% "circe-generic"    % circeVersion,
      "io.circe"       %% "circe-parser"     % circeVersion,
      "com.beachape"   %% "enumeratum-circe" % enumeratumCirceVersion,
      "org.scalatest"  %% "scalatest"        % scalatestVersion % Test,
      "com.gaborpihaj" %% "fetchfile"        % "0.2.0" % Test
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)
  )

lazy val root = (project in file("."))
  .settings(
    skip in publish := true
  )
  .aggregate(mtgjson)

addCommandAlias("prePush", ";scalafix ;test:scalafix ;scalafmtAll ;scalafmtSbt ;clean ;test")
