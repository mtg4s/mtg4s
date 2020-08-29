import xerial.sbt.Sonatype._

ThisBuild / name := "mtg4s"
ThisBuild / scalaVersion := "2.13.1"
ThisBuild / organization := "com.gaborpihaj"
ThisBuild / dynverSonatypeSnapshots := true
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.3.1-RC2"

ThisBuild / publishTo := sonatypePublishToBundle.value

ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")

val catsVersion = "2.1.1"
val catsEffectVersion = "2.1.2"
val fs2Version = "2.3.0"
val enumeratumVersion = "1.5.13"
val enumeratumCirceVersion = "1.5.23"
val http4sVersion = "0.21.5"
val circeVersion = "0.13.0"
val shapelessVersion = "2.3.3"
val monocleVersion = "2.0.3"
val kantanCsvVersion = "0.6.0"
val jlineVersion = "3.14.0"
val console4sVersion = "0.1.0"

val scalatestVersion = "3.1.1"
val scalatestScalacheckVersion = "3.1.1.1"
val scalaCheckVersion = "1.14.3"

lazy val publishSettings = List(
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  publishMavenStyle := true,
  sonatypeProjectHosting := Some(GitHubHosting("voidcontext", "mtg4s", "gabor.pihaj@gmail.com"))
)

lazy val defaultSettings = Seq(
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oDF"),
  addCompilerPlugin(scalafixSemanticdb),
  addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.11.0" cross CrossVersion.full),
  addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1")
)

lazy val core = (project in file("modules/core"))
  .settings(
    name := "mtg4s-core",
    publishSettings,
    defaultSettings,
    libraryDependencies ++= Seq(
      "org.typelevel"              %% "cats-core"       % catsVersion,
      "com.chuusai"                %% "shapeless"       % shapelessVersion,
      "org.scalatest"              %% "scalatest"       % scalatestVersion % Test,
      "org.scalatestplus"          %% "scalacheck-1-14" % scalatestScalacheckVersion % Test,
      "org.scalacheck"             %% "scalacheck"      % scalaCheckVersion % Test,
      "com.github.julien-truffaut" %% "monocle-core"    % monocleVersion % Test
    )
  )

lazy val inventory = (project in file("modules/inventory"))
  .settings(
    name := "mtg4s-inventory",
    publishSettings,
    defaultSettings,
    libraryDependencies ++= Seq(
      "org.typelevel"              %% "cats-core"          % catsVersion,
      "org.typelevel"              %% "cats-effect"        % catsEffectVersion,
      "com.nrinaudo"               %% "kantan.csv"         % kantanCsvVersion,
      "com.github.julien-truffaut" %% "monocle-core"       % monocleVersion,
      "org.scalatest"              %% "scalatest"          % scalatestVersion % Test,
      "org.scalatestplus"          %% "scalacheck-1-14"    % scalatestScalacheckVersion % Test,
      "org.scalacheck"             %% "scalacheck"         % scalaCheckVersion % Test,
      "io.chrisdavenport"          %% "cats-scalacheck"    % "0.2.0" % Test,
      "com.nrinaudo"               %% "kantan.csv-generic" % kantanCsvVersion % Test,
      "org.typelevel"              %% "claimant"           % "0.1.3" % Test
    )
  )
  .dependsOn(core % "compile->compile;test->test")

lazy val mtgjson = (project in file("modules/mtgjson"))
  .settings(
    name := "mtg4s-mtgjson",
    publishSettings,
    defaultSettings,
    parallelExecution in Test := false,
    libraryDependencies ++= Seq(
      "org.typelevel"              %% "cats-core"            % catsVersion,
      "org.typelevel"              %% "cats-effect"          % catsEffectVersion,
      "com.beachape"               %% "enumeratum"           % enumeratumVersion,
      "com.github.julien-truffaut" %% "monocle-core"         % monocleVersion,
      "io.circe"                   %% "circe-core"           % circeVersion,
      "io.circe"                   %% "circe-generic"        % circeVersion,
      "io.circe"                   %% "circe-generic-extras" % circeVersion,
      "io.circe"                   %% "circe-parser"         % circeVersion,
      "com.beachape"               %% "enumeratum-circe"     % enumeratumCirceVersion,
      "org.scalatest"              %% "scalatest"            % scalatestVersion % Test
    )
  )
  .dependsOn(
    core,
    `mtgjson-allprintings` % "compile->test"
  )

lazy val `mtgjson-allprintings` = (project in file("modules/mtgjson-allprintings"))
  .settings(
    name := "mtg4s-mtgjson-allprintings",
    publishSettings,
    defaultSettings,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core"   % catsVersion,
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "org.scalatest" %% "scalatest"   % scalatestVersion % Test
    )
  )

lazy val `mtgjson-allprintings-mirror` = (project in file("modules/mtgjson-allprintings/mirror"))
  .settings(
    name := "mtg4s-mtgjson-allprintings-mirror",
    defaultSettings,
    skip in publish := true,
    libraryDependencies ++= Seq(
      "org.typelevel"      %% "cats-core"           % catsVersion,
      "org.typelevel"      %% "cats-effect"         % catsEffectVersion,
      "com.gaborpihaj"     %% "fetch-file"          % "0.3.0",
      "com.gaborpihaj"     %% "fetch-file-http4s"   % "0.3.0",
      "org.http4s"         %% "http4s-dsl"          % http4sVersion,
      "org.http4s"         %% "http4s-blaze-client" % http4sVersion,
      "org.http4s"         %% "http4s-circe"        % http4sVersion,
      "io.circe"           %% "circe-generic"       % "0.13.0",
      "com.github.seratch" %% "awscala-s3"          % "0.8.4",
      "co.fs2"             %% "fs2-core"            % fs2Version,
      "co.fs2"             %% "fs2-io"              % fs2Version
    )
  )
  .dependsOn(`mtgjson-allprintings`)

lazy val `terminal-extras` = (project in file("modules/terminal"))
  .settings(
    name := "mtg4s-terminal-extras",
    publishSettings,
    defaultSettings,
    libraryDependencies ++= Seq(
      "org.typelevel"              %% "cats-core"         % catsVersion,
      "org.typelevel"              %% "cats-effect"       % catsEffectVersion,
      "com.gaborpihaj"             %% "console4s"         % console4sVersion,
      "com.github.julien-truffaut" %% "monocle-core"      % monocleVersion,
      "com.gaborpihaj"             %% "console4s-testkit" % console4sVersion % Test,
      "org.scalatest"              %% "scalatest"         % scalatestVersion % Test
    )
  )
  .dependsOn(core % "compile->compile;test->test")

lazy val root = (project in file("."))
  .settings(
    skip in publish := true
  )
  .aggregate(
    core,
    inventory,
    mtgjson,
    `mtgjson-allprintings`,
    `mtgjson-allprintings-mirror`,
    `terminal-extras`
  )

addCommandAlias("fmt", ";scalafix ;test:scalafix ;scalafmtAll ;scalafmtSbt")
addCommandAlias("prePush", ";fmt ;clean ;reload ;test")
addCommandAlias("update-mirror", "mtgjson-allprintings-mirror/runMain vdx.mtg4s.mtgjson.UpdateMirror")
addCommandAlias("fetch-mtgjson", "mtgjson-allprintings-mirror/runMain vdx.mtg4s.mtgjson.FetchMirrored")
