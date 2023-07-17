package com.ossmatters.github.crawler

import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

import zio.*
import zio.interop.catz.*

import cats.data.NonEmptyList
import cats.syntax.all.*
import com.github.api.Repository
import doobie.*
import doobie.implicits.*
import doobie.implicits.javasql.TimestampMeta

import com.ossmatters.github.crawler.PersistenceLive.Queries.*
import com.ossmatters.shared.github.FetchState
import com.ossmatters.shared.github.GithubIssue
import com.ossmatters.shared.github.GithubIssueEvent
import com.ossmatters.shared.github.GithubIssueType
import com.ossmatters.shared.github.GithubRepository
import com.ossmatters.shared.github.RepositoryName

trait Persistence:
  def saveRepository(repository: GithubRepository): Task[Unit]
  def saveIssues(issues: List[GithubIssue]): Task[Unit]
  def saveIssueEvents(events: List[GithubIssueEvent]): Task[Unit]
  def getRepositories(names: NonEmptyList[RepositoryName]): Task[List[GithubRepository]]
  def getIssuesToFetchEvents(repositoryId: String): Task[List[GithubIssue]]

final class PersistenceLive(xa: Transactor[Task]) extends Persistence:

  override def saveRepository(repository: GithubRepository): Task[Unit] =
    upsertRepository(repository).run.void.transact(xa)

  override def saveIssues(issues: List[GithubIssue]): Task[Unit] =
    upsertIssues(issues).transact(xa).void

  override def saveIssueEvents(events: List[GithubIssueEvent]): Task[Unit] =
    upsertIssueEvents(events).transact(xa).void

  override def getRepositories(names: NonEmptyList[RepositoryName]): Task[List[GithubRepository]] =
    selectRepositories(names).to[List].transact(xa)

  override def getIssuesToFetchEvents(repositoryId: String): Task[List[GithubIssue]] =
    selectIssuesWithMoreEvents(repositoryId).to[List].transact(xa)

object PersistenceLive:
  val layer = ZLayer.fromFunction(PersistenceLive(_))

  object Queries:
    private val utc = ZoneId.of("UTC")
    private given Meta[Instant] = Meta[Timestamp].timap(_.toLocalDateTime.atZone(utc).toInstant) { i =>
      Timestamp.valueOf(LocalDateTime.ofInstant(i, utc))
    }
    private given Meta[GithubIssueType] = Meta[String].timap(GithubIssueType.valueOf)(_.toString)
    private given Meta[FetchState]      = Meta[String].timap(FetchState.valueOf)(_.toString)

    def upsertRepository(repository: GithubRepository): Update0 =
      import repository.*

      sql"""
        insert into github_repositories (id, name, owner, fetch_state, sync_cutoff)
        values ($id, $name, $owner, $fetchState, $syncCutoff)
        on conflict (id) do update set name = excluded.name, owner = excluded.owner, fetch_state = excluded.fetch_state, sync_cutoff = excluded.sync_cutoff
      """.update

    def upsertIssues(issues: List[GithubIssue]): ConnectionIO[Int] =
      val sql = """
        insert into github_issues (id, type, repository_id, title, url, created_at, author, has_more_events)
        values (?, ?, ?, ?, ?, ?, ?, ?)
        on conflict (id) do update set title = excluded.title, url = excluded.url, has_more_events = excluded.has_more_events
      """
      Update[GithubIssue](sql).updateMany(issues)

    def upsertIssueEvents(events: List[GithubIssueEvent]): ConnectionIO[Int] =
      val sql = """
        insert into github_issue_events (id, issue_id, type, created_at, author)
        values (?, ?, ?, ?, ?)
        on conflict (id) do nothing
      """
      Update[GithubIssueEvent](sql).updateMany(events)

    def selectRepositories(names: NonEmptyList[RepositoryName]): Query0[GithubRepository] =
      val q = fr"""
        select id, name, owner, fetch_state, sync_cutoff
        from github_repositories
        where """ ++ Fragments.in(fr"(owner, name)", names.map(n => (n.owner, n.name)))
      q.query

    def selectIssuesWithMoreEvents(repositoryId: String): Query0[GithubIssue] =
      val q = fr"""
        select i.id, i.type, i.repository_id, i.title, i.url, i.created_at, i.author, i.has_more_events
        from github_issues i
        where i.has_more_events = true and i.repository_id = $repositoryId
      """
      q.query
