import sbt.librarymanagement.syntax._

object Dependencies {
  object V {
    val caliban    = "2.5.3"
    val cats       = "2.10.0"
    val doobie     = "1.0.0-RC4"
    val ducktape   = "0.1.11"
    val flyway     = "10.8.1"
    val jwtScala   = "10.0.0"
    val logback    = "1.4.8"
    val postgresql = "42.7.2"
    val sttpClient = "3.9.3"
    val zio        = "2.0.21"
    val zioCats    = "23.1.0.1"
    val zioConfig  = "4.0.1"
    val zioHttp    = "3.0.0-RC5"
    val zioJson    = "0.6.2"
    val zioLogging = "2.2.2"
  }

  val calibanClient     = "com.github.ghostdogpr"         %% "caliban-client"             % V.caliban
  val catsCore          = "org.typelevel"                 %% "cats-core"                  % V.cats
  val doobieCore        = "org.tpolecat"                  %% "doobie-core"                % V.doobie
  val doobieHikari      = "org.tpolecat"                  %% "doobie-hikari"              % V.doobie
  val doobiePostgres    = "org.tpolecat"                  %% "doobie-postgres"            % V.doobie
  val ducktape          = "io.github.arainko"             %% "ducktape"                   % V.ducktape
  val flyway            = "org.flywaydb"                   % "flyway-core"                % V.flyway
  val flywayPostgres    = "org.flywaydb"                   % "flyway-database-postgresql" % V.flyway
  val jwtZioJson        = "com.github.jwt-scala"          %% "jwt-zio-json"               % V.jwtScala
  val logback           = "ch.qos.logback"                 % "logback-classic"            % V.logback
  val postgresql        = "org.postgresql"                 % "postgresql"                 % V.postgresql
  val sttpClientZio     = "com.softwaremill.sttp.client3" %% "zio"                        % V.sttpClient
  val zio               = "dev.zio"                       %% "zio"                        % V.zio
  val zioConfig         = "dev.zio"                       %% "zio-config"                 % V.zioConfig
  val zioConfigTypesafe = "dev.zio"                       %% "zio-config-typesafe"        % V.zioConfig
  val zioHttp           = "dev.zio"                       %% "zio-http"                   % V.zioHttp
  val zioHttpHtmx       = "dev.zio"                       %% "zio-http-htmx"              % V.zioHttp
  val zioInteropCats    = "dev.zio"                       %% "zio-interop-cats"           % V.zioCats
  val zioJson           = "dev.zio"                       %% "zio-json"                   % V.zioJson
  val zioLogging        = "dev.zio"                       %% "zio-logging-slf4j"          % V.zioLogging
  val zioStreams        = "dev.zio"                       %% "zio-streams"                % V.zio

  val zioTest    = "dev.zio" %% "zio-test"     % V.zio % Test
  val zioTestSbt = "dev.zio" %% "zio-test-sbt" % V.zio % Test
}
