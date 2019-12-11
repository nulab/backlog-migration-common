
name := "backlog-migration-common"

libraryDependencies ++= Seq(
  "com.nulab-inc"         %  "backlog4j"        % "2.3.0",
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
