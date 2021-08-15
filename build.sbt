lazy val commonSettings = Seq(
  organization := "com.nulabinc",
  version := "0.4.0",
  scalaVersion := "2.13.6",
  scalacOptions ++= List(
    "-feature",
    "-unchecked",
    "-deprecation",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-Ymacro-annotations",
    "-Ywarn-unused"
  ),
  libraryDependencies ++= {
    val catsVersion     = "2.1.1"
    val monixVersion    = "3.2.2"
    val doobieVersion   = "0.9.4"
    val akkaVersion     = "2.6.10"
    val akkaHttpVersion = "10.2.6"
    Seq(
      "org.typelevel"        %% "cats-core"        % catsVersion,
      "org.typelevel"        %% "cats-kernel"      % catsVersion,
      "org.typelevel"        %% "cats-macros"      % catsVersion,
      "io.monix"             %% "monix"            % monixVersion,
      "io.monix"             %% "monix-eval"       % monixVersion,
      "io.monix"             %% "monix-reactive"   % monixVersion,
      "org.tpolecat"         %% "doobie-core"      % doobieVersion,
      "org.tpolecat"         %% "doobie-hikari"    % doobieVersion,
      "com.typesafe.akka"    %% "akka-actor"       % akkaVersion,
      "com.typesafe.akka"    %% "akka-stream"      % akkaVersion,
      "com.typesafe.akka"    %% "akka-slf4j"       % akkaVersion,
      "com.typesafe.akka"    %% "akka-http"        % akkaHttpVersion,
      "org.xerial"            % "sqlite-jdbc"      % "3.36.0.1",
      "com.nulab-inc"         % "backlog4j"        % "2.4.2",
      "com.github.mpilquist" %% "simulacrum"       % "0.19.0",
      "org.fusesource.jansi"  % "jansi"            % "1.18",
      "com.osinka.i18n"      %% "scala-i18n"       % "1.0.3",
      "ch.qos.logback"        % "logback-classic"  % "1.2.5",
      "com.typesafe"          % "config"           % "1.4.1",
      "com.google.inject"     % "guice"            % "4.2.2",
      "io.spray"             %% "spray-json"       % "1.3.6",
      "net.codingwell"       %% "scala-guice"      % "4.2.6",
      "io.lemonlabs"         %% "scala-uri"        % "2.3.1",
      "com.github.pathikrit" %% "better-files"     % "3.9.1",
      "com.chuusai"          %% "shapeless"        % "2.3.3",
      "org.apache.commons"    % "commons-csv"      % "1.9.0",
      "org.scalatest"        %% "scalatest"        % "3.2.9"       % Test,
      "org.tpolecat"         %% "doobie-scalatest" % doobieVersion % "test"
    )
  },
  // scalafix
  addCompilerPlugin(scalafixSemanticdb),
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision
)

lazy val core = (project in file("core")).settings(commonSettings)

lazy val importer = (project in file("importer")).settings(commonSettings).dependsOn(core)

lazy val common = (project in file("."))
  .settings(commonSettings)
  .settings(
    name := "backlog-migration-common"
  )
  .dependsOn(core, importer)
  .aggregate(core, importer)

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0"

addCommandAlias(
  "fixAll",
  "all compile:scalafix; test:scalafix; scalafmt; test:scalafmt; scalafmtSbt"
)
addCommandAlias(
  "checkAll",
  "compile:scalafix --check; test:scalafix --check; scalafmtCheck; test:scalafmtCheck; scalafmtSbtCheck"
)

Global / onChangedBuildSource := IgnoreSourceChanges
