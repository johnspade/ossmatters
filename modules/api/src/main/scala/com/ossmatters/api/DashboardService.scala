package com.ossmatters.api

import java.time.Instant
import java.time.LocalDate

import zio.*
import zio.http.template.*

import cats.data.NonEmptyList

import com.ossmatters.shared.GithubConfig

trait DashboardService:
  def getReport(startOpt: Option[LocalDate], endOpt: Option[LocalDate]): Task[Map[String, List[OssEvent]]]

  def getUserReport(
      startOpt: Option[LocalDate],
      endOpt: Option[LocalDate],
      user: String
  ): Task[Map[String, List[OssEvent]]]

final class DashboardServiceLive(ossRepository: OssRepository, githubConfig: GithubConfig) extends DashboardService:
  override def getReport(startOpt: Option[LocalDate], endOpt: Option[LocalDate]): Task[Map[String, List[OssEvent]]] =
    for
      clock <- ZIO.clock
      now   <- clock.instant
      cutoff = now.minus(7L, java.time.temporal.ChronoUnit.DAYS)
      start  = startOpt.map(_.atStartOfDay().atZone(UTC).toInstant()).getOrElse(cutoff)
      end    = endOpt.map(_.plusDays(1).atStartOfDay().atZone(UTC).toInstant()).getOrElse(now)
      result <- ossRepository
        .getIssues(start, end, githubConfig.usernames)
        .map: events =>
          events
            .groupBy(_.repository)
            .view
            .mapValues: events =>
              toOssEvents(events, start, end, now)
                .filter: i =>
                  githubConfig.usernames.toList.contains(i.author)
                .filter: i =>
                  i.dateTime.isAfter(start) && i.dateTime.isBefore(end)
                .toList
                .sortBy(_.dateTime)
                .groupBy: ev =>
                  (ev.author, ev.url)
                .view
                .mapValues: events =>
                  events.reduceLeft: (a, b) =>
                    OssEvent(
                      a.dateTime,
                      a.isPr,
                      a.author,
                      a.types.concatNel(b.types).distinct,
                      a.url,
                      a.title
                    )
                .values
                .toList
                .sortBy(_.dateTime)
            .toMap
            .filterNot(_._2.isEmpty)
    yield result

  def getUserReport(
      startOpt: Option[LocalDate],
      endOpt: Option[LocalDate],
      user: String
  ): Task[Map[String, List[OssEvent]]] =
    for
      clock <- ZIO.clock
      now   <- clock.instant
      cutoff = now.minus(7L, java.time.temporal.ChronoUnit.DAYS)
      start  = startOpt.map(_.atStartOfDay().atZone(UTC).toInstant()).getOrElse(cutoff)
      end    = endOpt.map(_.plusDays(1).atStartOfDay().atZone(UTC).toInstant()).getOrElse(now)
      result <- ossRepository
        .getUserEvents(start, end, user)
        .map: events =>
          events
            .groupBy(_.repository)
            .view
            .mapValues: events =>
              toOssEvents(events, start, end, now)
                .filter: i =>
                  i.author == user
                .filter: i =>
                  i.dateTime.isAfter(start) && i.dateTime.isBefore(end)
                .toList
                .sortBy(_.dateTime)
                .groupBy: ev =>
                  (ev.author, ev.url)
                .view
                .mapValues: events =>
                  events.reduceLeft: (a, b) =>
                    OssEvent(
                      a.dateTime,
                      a.isPr,
                      a.author,
                      a.types.concatNel(b.types).distinct,
                      a.url,
                      a.title
                    )
                .values
                .toList
                .sortBy(_.dateTime)
            .toMap
            .filterNot(_._2.isEmpty)
    yield result

  private def toOssEvents(events: List[RepositoryEvent], start: Instant, end: Instant, now: Instant) =
    events
      .filterNot(_.issueAuthor.contains("scala-steward"))
      .flatMap: event =>
        event match
          case RepositoryEvent(_, isPr, issueTitle, issueUrl, issueCreatedAt, issueAuthor, _, None, _, _) =>
            List(
              OssEvent(
                issueCreatedAt,
                isPr,
                issueAuthor.getOrElse("unknown"),
                NonEmptyList.of("CREATED_EVENT"),
                issueUrl,
                issueTitle
              )
            )
          case RepositoryEvent(
                _,
                isPr,
                issueTitle,
                issueUrl,
                issueCreatedAt,
                issueAuthor,
                _,
                Some(eventType),
                eventAuthor,
                eventCreatedAt
              ) =>
            (if issueCreatedAt.isAfter(start) && issueCreatedAt.isBefore(end) then
               List(
                 OssEvent(
                   issueCreatedAt,
                   isPr,
                   issueAuthor.getOrElse("unknown"),
                   NonEmptyList.of("CREATED_EVENT"),
                   issueUrl,
                   issueTitle
                 )
               )
             else List.empty) ++
              List(
                OssEvent(
                  eventCreatedAt.getOrElse(now),
                  isPr,
                  eventAuthor.getOrElse("unknown"),
                  NonEmptyList.of(eventType),
                  issueUrl,
                  issueTitle
                )
              )
      .toSet

object DashboardServiceLive:
  val layer = ZLayer.fromFunction(DashboardServiceLive(_, _))
