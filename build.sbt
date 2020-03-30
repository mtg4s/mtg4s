import xerial.sbt.Sonatype._

ThisBuild / name := "mtg4s"
ThisBuild / scalaVersion := "2.13.1"
ThisBuild / organization := "com.gaborpihaj"
ThisBuild / dynverSonatypeSnapshots := true

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

lazy val mtgjson = (project in file("mtgjson"))
  .settings(publishSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.beachape"   %% "enumeratum"       % enumeratumVersion,
      "io.circe"       %% "circe-core"       % circeVersion,
      "io.circe"       %% "circe-generic"    % circeVersion,
      "com.beachape"   %% "enumeratum-circe" % enumeratumCirceVersion,
      "org.scalatest"  %% "scalatest"        % scalatestVersion % Test,
      "io.circe"       %% "circe-parser"     % circeVersion % Test,
      "org.typelevel"  %% "cats-core"        % catsVersion % Test,
      "org.typelevel"  %% "cats-effect"      % catsEffectVersion % Test,
      "com.gaborpihaj" %% "fetchfile"        % "0.2.0" % Test
    )
  )

lazy val root = (project in file("."))
  .settings(
    skip in publish := true
  )
  .aggregate(mtgjson)

addCommandAlias("prePush", ";scalafmtAll ;scalafmtSbt ;clean ;test")
