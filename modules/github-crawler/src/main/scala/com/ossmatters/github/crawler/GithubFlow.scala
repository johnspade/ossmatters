package com.ossmatters.github.crawler

import zio.*
import zio.stream.*

import cats.data.NonEmptyList

import com.ossmatters.shared.GithubConfig
import com.ossmatters.shared.github.FetchEvent
import com.ossmatters.shared.github.FetchState
import com.ossmatters.shared.github.GithubRepository

trait GithubFlow:
  def mkStream(): ZStream[Any, Throwable, Unit]

final class GithubFlowLive(crawlerService: CrawlerService, persistence: Persistence, config: GithubConfig)
    extends GithubFlow:
  override def mkStream(): ZStream[Any, Throwable, Unit] =
    ZStream
      .fromZIO(persistence.getRepositories(config.repos))
      .mapZIO: repos =>
        val firstToFetch = config.repos
          .filterNot: n =>
            repos.exists: r =>
              n.owner == r.owner && n.name == r.name && r.fetchState == FetchState.Fetched
          .sortBy: r =>
            r.owner + "/" + r.name
          .headOption
        ZIO.debug(s"First to fetch: $firstToFetch") *>
          ZIO
            .foreach(firstToFetch): n =>
              repos
                .find: r =>
                  n.owner == r.owner && n.name == r.name
                .map: r =>
                  ZIO.succeed(r).asSome
                .getOrElse:
                  Clock.instant.flatMap: now =>
                    crawlerService.fetchRepository(n.name, n.owner, now)
            .map:
              _.flatten -> repos
      .flatMap:
        case (Some(repo), _) =>
          val state = repo.fetchState
          state match
            case FetchState.Repository =>
              ZStream
                .fromZIO(transitionRepo(repo, FetchEvent.RepositoryFetched))
                .flatMap: _ =>
                  mkStream()
            case FetchState.PullRequests =>
              ZStream
                .fromZIO(
                  crawlerService.fetchPullRequests(repo.id) *> transitionRepo(repo, FetchEvent.PullRequestsFetched)
                )
                .flatMap: _ =>
                  mkStream()
            case FetchState.Issues =>
              ZStream.fromZIO(
                crawlerService.fetchIssues(repo.id) *> transitionRepo(repo, FetchEvent.IssuesFetched)
              ) *> mkStream()
            case FetchState.IssueEvents =>
              ZStream.fromZIO(
                persistence
                  .getIssuesToFetchEvents(repo.id)
                  .flatMap: issues =>
                    crawlerService.fetchIssueEvents(
                      issues.map(i => GithubIssueEventsCursor(i, None))
                    ) *> transitionRepo(repo, FetchEvent.IssueEventsFetched)
              ) *> mkStream()
            case FetchState.Fetched => mkStream()
        case (None, repos) =>
          ZStream.fromZIO(
            ZIO.sleep(30.seconds) *>
              Clock.instant.flatMap: now =>
                val oldest = repos
                  .filter(_.fetchState == FetchState.Fetched)
                  .sortBy(_.syncCutoff)
                  .find(_.syncCutoff.isBefore(now.minus(12.hours)))
                ZIO.debug(s"Oldest: $oldest") *> ZIO.foreachDiscard(oldest): o =>
                  crawlerService.updateIssues(o) *>
                    persistence.saveRepository(o.copy(syncCutoff = now))
          ) *> mkStream()

  private def transitionRepo(repo: GithubRepository, event: FetchEvent) =
    persistence.saveRepository(repo.copy(fetchState = repo.fetchState.transition(event)))
end GithubFlowLive

object GithubFlowLive:
  val layer = ZLayer.fromFunction(new GithubFlowLive(_, _, _))
