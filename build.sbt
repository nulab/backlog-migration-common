
name := "backlog-migration-common"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  "com.nulab-inc"         %  "backlog4j"        % "2.3.0-SNAPSHOT",
  "org.fusesource.jansi"  %  "jansi"            % "1.17",
  "com.osinka.i18n"       %% "scala-i18n"       % "1.0.2",
  "ch.qos.logback"        %  "logback-classic"  % "1.2.3",
  "com.typesafe"          %  "config"           % "1.3.3",
  "com.google.inject"     %  "guice"            % "4.2.0",
  "io.spray"              %% "spray-json"       % "1.3.4",
  "net.codingwell"        %% "scala-guice"      % "4.2.0",
  "com.netaporter"        %% "scala-uri"        % "0.4.16",
  "com.github.pathikrit"  %% "better-files"     % "3.4.0",
  "org.scalatest"         %% "scalatest"        % "3.0.5"   % Test
)
