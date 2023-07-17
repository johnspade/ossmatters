package com.ossmatters.github.crawler

import java.time.Instant

import zio.Config.string
import zio.*
import zio.stream.*

import caliban.client.Operations.RootQuery
import caliban.client.SelectionBuilder
import io.github.arainko.ducktape.*

import com.ossmatters.shared.github.GithubIssue
import com.ossmatters.shared.github.GithubIssueEvent
import com.ossmatters.shared.github.GithubIssueEventOrphan
import com.ossmatters.shared.github.GithubPageInfo
import com.ossmatters.shared.github.GithubRateLimit
import com.ossmatters.shared.github.GithubRepository

trait CrawlerService:
  def fetchRepository(name: String, owner: String, now: Instant): Task[Option[GithubRepository]]
  def fetchPullRequests(repositoryId: String): Task[Unit]
  def fetchIssues(repositoryId: String): Task[Unit]
  def fetchIssueEvents(issues: List[GithubIssueEventsCursor]): Task[Unit]
  def updateIssues(repository: GithubRepository): Task[Unit]

final class CrawlerServiceLive(client: GithubApiClient, persistence: Persistence, token: String) extends CrawlerService:
  override def fetchRepository(name: String, owner: String, now: Instant): Task[Option[GithubRepository]] =
    client
      .execute(createRepositoryQuery(name, owner, now), token)
      .flatMap { repoOpt =>
        ZIO.foreach(repoOpt)(repo => persistence.saveRepository(repo).as(repo))
      }

  override def fetchPullRequests(repositoryId: String): Task[Unit] =
    def go(
        query: SelectionBuilder[
          RootQuery,
          (
              Option[(List[(GithubIssueEventsCursor, List[GithubIssueEventOrphan])], GithubPageInfo)],
              Option[GithubRateLimit]
          )
        ]
    ): ZIO[Any, Throwable, Unit] = {
      val result = client.execute(query, token)

      result.flatMap { result =>
        result match
          case (Some((prs, pageInfo)), Some(rateLimit)) =>
            val debugInfo =
              ZIO.debug(s"============= Rate limit: $rateLimit")
            val withPrIds = prs.map { (pr, events) =>
              pr -> events.map(event => event.into[GithubIssueEvent].transform(Field.const(_.issueId, pr.issue.id)))
            }.toMap
            persistence.saveIssues(withPrIds.keys.map(_.issue).toList) *>
              persistence.saveIssueEvents(withPrIds.values.toList.flatten) *>
              (if pageInfo.hasNextPage && rateLimit.remaining > 0 then
                 debugInfo *> ZIO.sleep(1.second) *> go(
                   createPRQuery(repositoryId, pageInfo.endCursor)
                 )
               else debugInfo) // todo sleep until rate limit resets
          case _ => ZIO.unit
      }
    }

    go(createPRQuery(repositoryId, None))

  override def fetchIssues(repositoryId: String): Task[Unit] =
    def go(
        query: SelectionBuilder[
          RootQuery,
          (
              Option[(List[(GithubIssueEventsCursor, List[GithubIssueEventOrphan])], GithubPageInfo)],
              Option[GithubRateLimit]
          )
        ]
    ): ZIO[Any, Throwable, Unit] = {
      val result = client.execute(query, token)

      result.flatMap { result =>
        result match
          case (Some((issues, pageInfo)), Some(rateLimit)) =>
            val debugInfo =
              ZIO.debug(s"============= Rate limit: $rateLimit")
            val withIssueIds = issues.map { (issue, events) =>
              issue -> events.map(event =>
                event.into[GithubIssueEvent].transform(Field.const(_.issueId, issue.issue.id))
              )
            }.toMap
            persistence.saveIssues(withIssueIds.keys.map(_.issue).toList) *>
              persistence.saveIssueEvents(withIssueIds.values.toList.flatten) *>
              (if pageInfo.hasNextPage && rateLimit.remaining > 0 then
                 debugInfo *> ZIO.sleep(1.second) *> go(
                   createIssueQuery(repositoryId, pageInfo.endCursor)
                 )
               else debugInfo) // todo sleep until rate limit resets
          case _ => ZIO.unit
      }
    }

    go(createIssueQuery(repositoryId, None))

  override def fetchIssueEvents(issues: List[GithubIssueEventsCursor]): Task[Unit] =
    def go(
        issueId: String,
        query: SelectionBuilder[
          RootQuery,
          (Option[(List[GithubIssueEventOrphan], GithubPageInfo)], Option[GithubRateLimit])
        ]
    ): ZIO[Any, Throwable, Unit] = {
      val result = client.execute(query, token)

      result.flatMap { result =>
        result match
          case (Some((issueEvents, pageInfo)), Some(rateLimit)) =>
            val debugInfo =
              ZIO.debug(s"============= Rate limit: $rateLimit")
            val events =
              issueEvents.map(event => event.into[GithubIssueEvent].transform(Field.const(_.issueId, issueId)))
            persistence.saveIssueEvents(events) *>
              (if pageInfo.hasNextPage && rateLimit.remaining > 0 then
                 debugInfo *> ZIO.sleep(1.second) *> go(issueId, createIssueEventsQuery(issueId, pageInfo.endCursor))
               else debugInfo) // todo sleep until rate limit resets
          case _ => ZIO.unit
      }
    }

    ZIO.debug(s"Issues to fetch more events: ${issues.map(_.issue.title)}") *> ZIO.foreachDiscard(issues)(issue =>
      go(issue.issue.id, createIssueEventsQuery(issue.issue.id, issue.cursor))
    )

  override def updateIssues(repository: GithubRepository): Task[Unit] =
    def go(
        query: SelectionBuilder[
          RootQuery,
          (List[(GithubIssueEventsCursor, List[GithubIssueEventOrphan])], GithubPageInfo, Option[GithubRateLimit])
        ],
        acc: List[GithubIssueEventsCursor]
    ): ZIO[Any, Throwable, List[GithubIssueEventsCursor]] =
      val result = client.execute(query, token)

      result.flatMap { result =>
        result match
          case (issues, pageInfo, Some(rateLimit)) =>
            val debugInfo =
              ZIO.debug(s"============= Rate limit: $rateLimit")
            val withIssueIds = issues.map { (issue, events) =>
              issue -> events.map(event =>
                event.into[GithubIssueEvent].transform(Field.const(_.issueId, issue.issue.id))
              )
            }.toMap
            persistence.saveIssues(withIssueIds.keys.map(_.issue).toList) *>
              persistence.saveIssueEvents(withIssueIds.values.toList.flatten) *>
              (if pageInfo.hasNextPage && rateLimit.remaining > 0 then
                 debugInfo *> ZIO.sleep(1.second) *> go(
                   searchIssuesUpdatedAfter(
                     repository.syncCutoff,
                     repository.id,
                     repository.fullName,
                     pageInfo.endCursor
                   ),
                   acc ++ withIssueIds.keys.toList
                 )
               else debugInfo *> ZIO.succeed(acc ++ withIssueIds.keys.toList)) // todo sleep until rate limit resets
          case _ => ZIO.succeed(acc)
      }

    go(searchIssuesUpdatedAfter(repository.syncCutoff, repository.id, repository.fullName, after = None), List.empty)
      .flatMap: issues =>
        ZIO.debug(s"Updated issues: ${issues.map(_.issue.title)}") *>
          fetchIssueEvents(issues.filter(_.issue.hasMoreEvents))

object CrawlerServiceLive:
  val layer = ZLayer(
    for
      client      <- ZIO.service[GithubApiClient]
      persistence <- ZIO.service[Persistence]
      token       <- ZIO.config(string("GITHUB_API_TOKEN"))
    yield CrawlerServiceLive(client, persistence, token)
  )
