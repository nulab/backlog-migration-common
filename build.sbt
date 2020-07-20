
lazy val commonSettings = Seq(
  organization := "com.nulabinc",
  version := "0.1.2-SNAPSHOT",
  scalaVersion := "2.13.3",
  scalacOptions ++= List(
    "-unchecked",
    "-deprecation",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-Ymacro-annotations"
  )
)

lazy val core = (project in file("core"))
  .settings(commonSettings)

lazy val importer = (project in file("importer"))
  .settings(commonSettings)
  .dependsOn(core)

lazy val common = (project in file("."))
  .settings(commonSettings)
  .settings(
    name := "backlog-migration-common",
  )
  .aggregate(core, importer)