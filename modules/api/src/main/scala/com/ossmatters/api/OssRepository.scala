package com.ossmatters.api

import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

import zio.*
import zio.interop.catz.*

import cats.data.NonEmptyList
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import doobie.implicits.javasql.TimestampMeta

import com.ossmatters.api.OssRepositoryLive.Queries.*
import com.ossmatters.shared.github.GithubIssueType

trait OssRepository:
  def getIssues(start: Instant, end: Instant, users: NonEmptyList[String]): Task[List[RepositoryEvent]]
  def getUserEvents(start: Instant, end: Instant, user: String): Task[List[RepositoryEvent]]

final class OssRepositoryLive(xa: Transactor[Task]) extends OssRepository:
  override def getIssues(start: Instant, end: Instant, users: NonEmptyList[String]): Task[List[RepositoryEvent]] =
    selectIssues(start, end, users).to[List].transact(xa)

  override def getUserEvents(start: Instant, end: Instant, user: String): Task[List[RepositoryEvent]] =
    selectUserEvents(start, end, user).to[List].transact(xa)

object OssRepositoryLive:
  val layer = ZLayer.fromFunction(OssRepositoryLive(_))

  object Queries:
    private given Meta[Instant] = Meta[Timestamp].timap(_.toLocalDateTime.atZone(UTC).toInstant): i =>
      Timestamp.valueOf(LocalDateTime.ofInstant(i, UTC))
    private given Meta[GithubIssueType] = Meta[String].timap(GithubIssueType.valueOf)(_.toString)

    def selectIssues(start: Instant, end: Instant, users: NonEmptyList[String]) =
      val q = fr"""
        select i.id, i.type = ${GithubIssueType.PullRequest} as is_pr, i.title, i.url, i.created_at, i.author, r.owner || '/' || r.name as repo, e.type, e.author, min(e.created_at) as created_at
        from github_issues i
        join github_repositories r on i.repository_id = r.id
        join github_issue_events e on i.id = e.issue_id
        where (i.created_at >= $start or e.created_at >= $start) and (i.created_at <= $end or e.created_at <= $end) and """ ++
        Fragments.or(
          Fragments.in(fr"i.author", users),
          Fragments.in(fr"e.author", users)
        )
        ++ fr"and not (i.type = ${GithubIssueType.PullRequest} and e.type = 'CLOSED_EVENT')"
        ++ fr"group by i.id, is_pr, i.title, i.url, i.created_at, i.author, repo, e.type, e.author"
      q.query[RepositoryEvent]

    def selectUserEvents(start: Instant, end: Instant, user: String) =
      val q = fr"""
        select i.id, i.type = ${GithubIssueType.PullRequest} as is_pr, i.title, i.url, i.created_at, i.author, r.owner || '/' || r.name as repo, e.type, e.author, e.created_at
        from github_issues i
        join github_repositories r on i.repository_id = r.id
        join github_issue_events e on i.id = e.issue_id
        where (i.created_at >= $start or e.created_at >= $start) and (i.created_at <= $end or e.created_at <= $end) and """ ++
        Fragments.or(
          fr"i.author = $user",
          fr"e.author = $user"
        )
      q.query[RepositoryEvent]
