
name := "backlog-migration-common"

unmanagedBase := baseDirectory.value / "libs"

libraryDependencies ++= Seq(
  "org.fusesource.jansi"  %  "jansi"            % "1.17",
  "com.osinka.i18n"       %% "scala-i18n"       % "1.0.2",
  "ch.qos.logback"        %  "logback-classic"  % "1.2.3",
  "com.typesafe"          %  "config"           % "1.3.3",
  "com.google.inject"     %  "guice"            % "4.2.0",
  "io.spray"              %% "spray-json"       % "1.3.4",
  "com.mixpanel"          %  "mixpanel-java"    % "1.4.4",
  "net.codingwell"        %% "scala-guice"      % "4.2.0",
  "com.netaporter"        %% "scala-uri"        % "0.4.16",
  "com.github.pathikrit"  %% "better-files"     % "3.4.0",
  "org.scalatest"         %% "scalatest"        % "3.0.5"   % Test
)
