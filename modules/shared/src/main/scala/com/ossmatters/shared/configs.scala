package com.ossmatters.shared

import java.time.ZoneId

import zio.*
import zio.config.*
import zio.config.typesafe.*

import cats.data.NonEmptyList

import com.ossmatters.shared.github.RepositoryName

import Config.*

final case class GithubConfig(
    repos: NonEmptyList[RepositoryName],
    usernames: NonEmptyList[String]
)

object GithubConfig:
  val descriptor: Config[GithubConfig] =
    (listOf[String]("repos", string) zip listOf[String]("usernames", string))
      .map: (repos, usernames) =>
        GithubConfig(
          NonEmptyList
            .fromListUnsafe(
              repos.sorted.map: r =>
                r.split("/").toList match
                  case owner :: name :: Nil => RepositoryName(owner, name)
                  case _                    => throw new IllegalArgumentException(s"Invalid repository name: $r")
            ),
          NonEmptyList.fromListUnsafe(usernames).sorted
        )

  val layer = ZLayer(
    ZIO
      .config(string("GITHUB_CONFIG_PATH"))
      .flatMap(path => ConfigProvider.fromHoconFilePath(path).load(descriptor).orDie)
  )
