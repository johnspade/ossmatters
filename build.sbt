import Dependencies._
import scala.sys.process.Process

val scala3Version = "3.3.0"

ThisBuild / organization    := "com.ossmatters"
ThisBuild / scalaVersion    := scala3Version
ThisBuild / dynverSeparator := "-"

Global / onChangedBuildSource := ReloadOnSourceChanges

val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "utf8"
  ),
  resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision
)

lazy val root = project
  .in(file("."))
  .settings(name := "ossmatters")
  .aggregate(server, api, githubApi, githubCrawler, shared)

lazy val api = project
  .in(file("modules/api"))
  .dependsOn(shared, githubCrawler)
  .settings(commonSettings)
  .settings(
    name := "api",
    libraryDependencies ++= Seq(
      doobieCore,
      doobieHikari,
      doobiePostgres,
      zio,
      zioInteropCats,
      zioConfig,
      zioLogging,
      zioTest,
      zioTestSbt,
      flyway,
      flywayPostgres,
      jwtZioJson,
      zioConfig,
      zioHttp,
      zioHttpHtmx,
      zioJson,
      zioLogging,
      logback,
      postgresql
    )
  )

lazy val githubApi = project
  .in(file("modules/github-api"))
  .enablePlugins(CalibanPlugin)
  .disablePlugins(ScalafixPlugin)
  .settings(
    name := "github-api",
    libraryDependencies ++= Seq(
      zio,
      calibanClient
    ),
    Compile / caliban / calibanSettings += calibanSetting(file("Github.graphql"))(
      _.packageName("com.github.api").splitFiles(true)
    )
  )

lazy val githubCrawler = project
  .in(file("modules/github-crawler"))
  .dependsOn(shared, githubApi)
  .settings(commonSettings)
  .settings(
    name := "github",
    libraryDependencies ++= Seq(
      doobieCore,
      doobiePostgres,
      ducktape,
      zio,
      zioInteropCats,
      zioStreams,
      zioConfig,
      zioHttp,
      zioJson,
      zioLogging,
      logback,
      zioTest,
      zioTestSbt,
      sttpClientZio
    )
  )

lazy val server = project
  .in(file("modules/server"))
  .dependsOn(api, githubCrawler)
  .settings(commonSettings)
  .settings(
    name := "ossmatters-server",
    libraryDependencies ++= Seq(
      zio,
      zioStreams,
      zioConfig
    ),
    jibBaseImage    := "eclipse-temurin:17.0.10_7-jre",
    jibOrganization := "johnspade",
    jibName         := "ossmatters",
    jibRegistry     := "registry.gitlab.com"
  )

lazy val shared = project
  .in(file("modules/shared"))
  .settings(commonSettings)
  .settings(
    name := "shared",
    libraryDependencies ++= Seq(
      catsCore,
      zio,
      zioConfig,
      zioConfigTypesafe
    )
  )

addCommandAlias("validate", ";compile;Test/compile;scalafmtCheck;Test/scalafmtCheck;test")

Global / onChangedBuildSource := ReloadOnSourceChanges
