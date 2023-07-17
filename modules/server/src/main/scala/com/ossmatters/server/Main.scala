package com.ossmatters.server

import zio.*
import zio.http.Client
import zio.stream.*

import com.ossmatters.api.AuthServiceLive
import com.ossmatters.api.DashboardServiceLive
import com.ossmatters.api.DbConfig
import com.ossmatters.api.DbTransactor
import com.ossmatters.api.FlywayMigration
import com.ossmatters.api.OssMattersServer
import com.ossmatters.api.OssRepositoryLive
import com.ossmatters.api.Routes
import com.ossmatters.github.crawler.CrawlerService
import com.ossmatters.github.crawler.CrawlerServiceLive
import com.ossmatters.github.crawler.GithubApiClientLive
import com.ossmatters.github.crawler.GithubFlow
import com.ossmatters.github.crawler.GithubFlowLive
import com.ossmatters.github.crawler.PersistenceLive
import com.ossmatters.shared.GithubConfig

object Main extends ZIOAppDefault:
  def run: Task[Unit] =
    (for
      _      <- FlywayMigration.migrate
      stream <- ZIO.serviceWith[GithubFlow](_.mkStream())
      _      <- stream.runDrain.zipPar(ZIO.serviceWithZIO[OssMattersServer](_.start))
    yield ())
      .provide(
        GithubConfig.layer,
        Client.default,
        GithubApiClientLive.layer,
        AuthServiceLive.layer,
        CrawlerServiceLive.layer,
        PersistenceLive.layer,
        DbTransactor.live,
        GithubFlowLive.layer,
        OssRepositoryLive.layer,
        DashboardServiceLive.layer,
        OssMattersServer.layer,
        Routes.layer
      )
