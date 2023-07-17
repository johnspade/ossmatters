package com.ossmatters.github.crawler

import zio.*

import caliban.client.Operations.RootQuery
import caliban.client.SelectionBuilder
import sttp.capabilities.zio.ZioStreams
import sttp.client3.SttpBackend
import sttp.client3.*
import sttp.client3.httpclient.zio.HttpClientZioBackend

trait GithubApiClient:
  def execute[A](query: SelectionBuilder[RootQuery, A], token: String): Task[A]

final class GithubApiClientLive(backend: SttpBackend[Task, ZioStreams]) extends GithubApiClient:
  private val serverUrl = uri"https://api.github.com/graphql"

  override def execute[A](query: SelectionBuilder[RootQuery, A], token: String): Task[A] =
    ZIO.debug(query.toGraphQL().query) *>
      query
        .toRequest(serverUrl)
        .header("X-Github-Next-Global-ID", "1")
        .auth
        .bearer(token)
        .send(backend)
        .retry(Schedule.recurs(3) && Schedule.spaced(1.second))
        .map(_.body)
        .absolve

object GithubApiClientLive:
  val layer: ZLayer[Any, Throwable, GithubApiClientLive] =
    ZLayer(
      for client <- HttpClientZioBackend()
      yield GithubApiClientLive(client)
    )
