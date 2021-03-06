lazy val commonSettings = Seq(
  organization := "com.nulabinc",
  version := "0.3.5-SNAPSHOT",
  scalaVersion := "2.13.5",
  scalacOptions ++= List(
    "-unchecked",
    "-deprecation",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-Ymacro-annotations"
  ),
  libraryDependencies ++= {
    val catsVersion     = "2.1.0"
    val monixVersion    = "3.2.2"
    val doobieVersion   = "0.9.0"
    val akkaVersion     = "2.6.10"
    val akkaHttpVersion = "10.2.1"
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
      "org.xerial"            % "sqlite-jdbc"      % "3.32.3.2",
      "com.nulab-inc"         % "backlog4j"        % "2.4.2",
      "com.github.mpilquist" %% "simulacrum"       % "0.19.0",
      "org.fusesource.jansi"  % "jansi"            % "1.17",
      "com.osinka.i18n"      %% "scala-i18n"       % "1.0.3",
      "ch.qos.logback"        % "logback-classic"  % "1.2.3",
      "com.typesafe"          % "config"           % "1.3.3",
      "com.google.inject"     % "guice"            % "4.2.2",
      "io.spray"             %% "spray-json"       % "1.3.5",
      "net.codingwell"       %% "scala-guice"      % "4.2.6",
      "io.lemonlabs"         %% "scala-uri"        % "2.0.0",
      "com.github.pathikrit" %% "better-files"     % "3.8.0",
      "com.chuusai"          %% "shapeless"        % "2.3.3",
      "org.apache.commons"    % "commons-csv"      % "1.5",
      "org.scalatest"        %% "scalatest"        % "3.1.0"       % Test,
      "org.tpolecat"         %% "doobie-scalatest" % doobieVersion % "test"
    )
  }
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
