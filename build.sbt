lazy val commonSettings = Seq(
  organization := "com.nulabinc",
  version      := "0.6.0",
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
  javacOptions ++= Seq("-encoding", "UTF-8", "-source", "11", "-target", "11"),
  libraryDependencies ++= {
    val catsVersion      = "2.8.0"
    val monixVersion     = "3.2.2"
    val doobieVersion    = "0.9.0"
    val pekkoVersion     = "1.0.3"
    val pekkoHttpVersion = "1.0.1"
    Seq(
      "org.typelevel"        %% "cats-core"        % catsVersion,
      "org.typelevel"        %% "cats-kernel"      % catsVersion,
      "io.monix"             %% "monix"            % monixVersion,
      "io.monix"             %% "monix-eval"       % monixVersion,
      "io.monix"             %% "monix-reactive"   % monixVersion,
      "org.tpolecat"         %% "doobie-core"      % doobieVersion,
      "org.tpolecat"         %% "doobie-hikari"    % doobieVersion,
      "org.apache.pekko"     %% "pekko-actor"      % pekkoVersion,
      "org.apache.pekko"     %% "pekko-stream"     % pekkoVersion,
      "org.apache.pekko"     %% "pekko-slf4j"      % pekkoVersion,
      "org.apache.pekko"     %% "pekko-http"       % pekkoHttpVersion,
      "org.xerial"            % "sqlite-jdbc"      % "3.36.0.3",
      "com.nulab-inc"         % "backlog4j"        % "2.6.0",
      "org.typelevel"        %% "simulacrum"       % "1.0.0",
      "org.fusesource.jansi"  % "jansi"            % "2.4.0",
      "com.osinka.i18n"      %% "scala-i18n"       % "1.0.3",
      "ch.qos.logback"        % "logback-classic"  % "1.2.7",
      "com.typesafe"          % "config"           % "1.4.1",
      "com.google.inject"     % "guice"            % "5.0.1",
      "io.spray"             %% "spray-json"       % "1.3.6",
      "net.codingwell"       %% "scala-guice"      % "5.0.2",
      "io.lemonlabs"         %% "scala-uri"        % "4.0.3",
      "com.github.pathikrit" %% "better-files"     % "3.9.1",
      "com.chuusai"          %% "shapeless"        % "2.3.7",
      "org.apache.commons"    % "commons-csv"      % "1.9.0",
      "org.scalatest"        %% "scalatest"        % "3.1.0"       % Test,
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

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"

addCommandAlias(
  "fixAll",
  "all compile:scalafix; test:scalafix; scalafmt; test:scalafmt; scalafmtSbt"
)
addCommandAlias(
  "checkAll",
  "compile:scalafix --check; test:scalafix --check; scalafmtCheck; test:scalafmtCheck; scalafmtSbtCheck"
)

Global / onChangedBuildSource := IgnoreSourceChanges
