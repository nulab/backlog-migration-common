
name := "backlog-migration-common"

val catsVersion = "2.0.0"
val monixVersion = "3.1.0"

libraryDependencies ++= Seq(
  "org.typelevel"         %% "cats-core"        % catsVersion,
  "org.typelevel"         %% "cats-kernel"      % catsVersion,
  "org.typelevel"         %% "cats-macros"      % catsVersion,
  "io.monix"              %% "monix"            % monixVersion,
  "io.monix"              %% "monix-eval"       % monixVersion,
  "io.monix"              %% "monix-reactive"   % monixVersion,
  "com.nulab-inc"         %  "backlog4j"        % "2.3.3",
  "org.fusesource.jansi"  %  "jansi"            % "1.17",
  "com.osinka.i18n"       %% "scala-i18n"       % "1.0.3",
  "ch.qos.logback"        %  "logback-classic"  % "1.2.3",
  "com.typesafe"          %  "config"           % "1.3.3",
  "com.google.inject"     %  "guice"            % "4.2.0",
  "io.spray"              %% "spray-json"       % "1.3.5",
  "net.codingwell"        %% "scala-guice"      % "4.2.6",
  "io.lemonlabs"          %% "scala-uri"        % "1.5.1",
  "com.github.pathikrit"  %% "better-files"     % "3.8.0",
  "org.scalatest"         %% "scalatest"        % "3.0.8"   % Test
)
