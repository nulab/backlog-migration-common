
name := "backlog-migration-common"

val catsVersion = "2.1.0"
val monixVersion = "3.1.0"
val slickVersion = "3.3.2"

libraryDependencies ++= Seq(
  "org.typelevel"         %% "cats-core"        % catsVersion,
  "org.typelevel"         %% "cats-kernel"      % catsVersion,
  "org.typelevel"         %% "cats-macros"      % catsVersion,
  "io.monix"              %% "monix"            % monixVersion,
  "io.monix"              %% "monix-eval"       % monixVersion,
  "io.monix"              %% "monix-reactive"   % monixVersion,
  "com.typesafe.slick"    %% "slick"            % slickVersion,
  "com.typesafe.slick"    %% "slick-hikaricp"   % slickVersion,
  "org.xerial"            %  "sqlite-jdbc"      % "3.30.1",
  "com.nulab-inc"         %  "backlog4j"        % "2.3.3",
  "com.github.mpilquist"  %% "simulacrum"       % "0.19.0",
  "org.fusesource.jansi"  %  "jansi"            % "1.17",
  "com.osinka.i18n"       %% "scala-i18n"       % "1.0.3",
  "ch.qos.logback"        %  "logback-classic"  % "1.2.3",
  "com.typesafe"          %  "config"           % "1.3.3",
  "com.google.inject"     %  "guice"            % "4.2.2",
  "io.spray"              %% "spray-json"       % "1.3.5",
  "net.codingwell"        %% "scala-guice"      % "4.2.6",
  "io.lemonlabs"          %% "scala-uri"        % "2.0.0",
  "com.github.pathikrit"  %% "better-files"     % "3.8.0",
  "com.chuusai"           %% "shapeless"        % "2.3.3",
  "org.scalatest"         %% "scalatest"        % "3.1.0"   % Test
)
