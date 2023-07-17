package com.ossmatters.github.crawler

import java.time.Instant

import caliban.client.Operations.RootQuery
import caliban.client.SelectionBuilder
import com.github.api.*

import com.ossmatters.shared.github.FetchState
import com.ossmatters.shared.github.GithubIssue
import com.ossmatters.shared.github.GithubIssueEventOrphan
import com.ossmatters.shared.github.GithubIssueType
import com.ossmatters.shared.github.GithubPageInfo
import com.ossmatters.shared.github.GithubRateLimit
import com.ossmatters.shared.github.GithubRepository

def createRepositoryQuery(
    name: String,
    owner: String,
    syncCutoff: Instant
): SelectionBuilder[RootQuery, Option[GithubRepository]] =
  Query
    .repository(name = name, owner = owner) {
      Repository.id
        .map { case id =>
          GithubRepository(id, name, owner, FetchState.Repository, syncCutoff)
        }
    }

val PullRequestTimelineItemsItemTypes: List[PullRequestTimelineItemsItemType] = List(
  PullRequestTimelineItemsItemType.CLOSED_EVENT,
  PullRequestTimelineItemsItemType.ISSUE_COMMENT,
  // LABELED_EVENT,
  PullRequestTimelineItemsItemType.MERGED_EVENT,
  PullRequestTimelineItemsItemType.PULL_REQUEST_COMMIT,
  PullRequestTimelineItemsItemType.PULL_REQUEST_REVIEW
  // READY_FOR_REVIEW_EVENT,
  // REVIEW_REQUESTED_EVENT,
)

val IssueTimelineItemsItemTypes: List[IssueTimelineItemsItemType] = List(
  IssueTimelineItemsItemType.CLOSED_EVENT,
  IssueTimelineItemsItemType.ISSUE_COMMENT
)

def issueEventToDomain(
    id: String,
    createdAt: String,
    authorOpt: Option[String],
    `type`: String
): GithubIssueEventOrphan =
  GithubIssueEventOrphan(id, `type`, Instant.parse(createdAt), authorOpt)

// scalafmt: { maxColumn = 999 }
def pullRequestEvents() =
  def eventToDomain[A](
      sb: SelectionBuilder[A, (String, String, Option[String])],
      `type`: PullRequestTimelineItemsItemType
  ): SelectionBuilder[A, GithubIssueEventOrphan] =
    sb.mapN[String, String, Option[String], GithubIssueEventOrphan]((id, createdAt, authorOpt) => issueEventToDomain(id, createdAt, authorOpt, `type`.toString()))

  PullRequestTimelineItemsConnection
    .nodesOption[GithubIssueEventOrphan](
      onClosedEvent = Some(eventToDomain(ClosedEvent.id ~ ClosedEvent.createdAt ~ ClosedEvent.actorInterface(Actor.login), PullRequestTimelineItemsItemType.CLOSED_EVENT)),
      // onCommentDeletedEvent = Some(eventToDomain(CommentDeletedEvent.id ~ CommentDeletedEvent.createdAt ~ CommentDeletedEvent.actorInterface(Actor.login), COMMENT_DELETED_EVENT)),
      // onConvertToDraftEvent = Some(eventToDomain(ConvertToDraftEvent.id ~ ConvertToDraftEvent.createdAt ~ ConvertToDraftEvent.actorInterface(Actor.login), CONVERT_TO_DRAFT_EVENT)),
      // onCrossReferencedEvent = Some(eventToDomain(CrossReferencedEvent.id ~ CrossReferencedEvent.createdAt ~ CrossReferencedEvent.actorInterface(Actor.login), CROSS_REFERENCED_EVENT)),
      onIssueComment = Some(eventToDomain(IssueComment.id ~ IssueComment.createdAt ~ IssueComment.authorInterface(Actor.login), PullRequestTimelineItemsItemType.ISSUE_COMMENT)),
      // onLabeledEvent = Some(eventToDomain(LabeledEvent.id ~ LabeledEvent.createdAt ~ LabeledEvent.actorInterface(Actor.login), LABELED_EVENT)),
      // onLockedEvent = Some(eventToDomain(LockedEvent.id ~ LockedEvent.createdAt ~ LockedEvent.actorInterface(Actor.login), LOCKED_EVENT)),
      // onMarkedAsDuplicateEvent = Some(eventToDomain(MarkedAsDuplicateEvent.id ~ MarkedAsDuplicateEvent.createdAt ~ MarkedAsDuplicateEvent.actorInterface(Actor.login), MARKED_AS_DUPLICATE_EVENT)),
      onMergedEvent = Some(eventToDomain(MergedEvent.id ~ MergedEvent.createdAt ~ MergedEvent.actorInterface(Actor.login), PullRequestTimelineItemsItemType.MERGED_EVENT)),
      onPullRequestCommit = Some(
        (PullRequestCommit.id ~ PullRequestCommit.commit(Commit.authoredDate ~ Commit.author(GitActor.user(User.login))))
          .mapN[String, (String, Option[Option[String]]), GithubIssueEventOrphan]((id, commit) => GithubIssueEventOrphan(id, PullRequestTimelineItemsItemType.PULL_REQUEST_COMMIT.toString(), Instant.parse(commit._1), commit._2.flatten))
      ),
      onPullRequestReview = Some(eventToDomain(PullRequestReview.id ~ PullRequestReview.createdAt ~ PullRequestReview.authorInterface(Actor.login), PullRequestTimelineItemsItemType.PULL_REQUEST_REVIEW))
      // onReadyForReviewEvent = Some(eventToDomain(ReadyForReviewEvent.id ~ ReadyForReviewEvent.createdAt ~ ReadyForReviewEvent.actorInterface(Actor.login), READY_FOR_REVIEW_EVENT)),
      // onRenamedTitleEvent = Some(eventToDomain(RenamedTitleEvent.id ~ RenamedTitleEvent.createdAt ~ RenamedTitleEvent.actorInterface(Actor.login), RENAMED_TITLE_EVENT)),
      // onReopenedEvent = Some(eventToDomain(ReopenedEvent.id ~ ReopenedEvent.createdAt ~ ReopenedEvent.actorInterface(Actor.login), REOPENED_EVENT)),
      // onReviewDismissedEvent = Some(eventToDomain(ReviewDismissedEvent.id ~ ReviewDismissedEvent.createdAt ~ ReviewDismissedEvent.actorInterface(Actor.login), REVIEW_DISMISSED_EVENT)),
      // onReviewRequestedEvent = Some(eventToDomain(ReviewRequestedEvent.id ~ ReviewRequestedEvent.createdAt ~ ReviewRequestedEvent.actorInterface(Actor.login), REVIEW_REQUESTED_EVENT)),
      // onUnassignedEvent = Some(eventToDomain(UnassignedEvent.id ~ UnassignedEvent.createdAt ~ UnassignedEvent.actorInterface(Actor.login), UNASSIGNED_EVENT)),
      // onUnlabeledEvent = Some(eventToDomain(UnlabeledEvent.id ~ UnlabeledEvent.createdAt ~ UnlabeledEvent.actorInterface(Actor.login), UNLABELED_EVENT)),
      // onUnlockedEvent = Some(eventToDomain(UnlockedEvent.id ~ UnlockedEvent.createdAt ~ UnlockedEvent.actorInterface(Actor.login), UNLOCKED_EVENT)),
      // onUnmarkedAsDuplicateEvent = Some(eventToDomain(UnmarkedAsDuplicateEvent.id ~ UnmarkedAsDuplicateEvent.createdAt ~ UnmarkedAsDuplicateEvent.actorInterface(Actor.login), UNMARKED_AS_DUPLICATE_EVENT)),
      // onUnpinnedEvent = Some(eventToDomain(UnpinnedEvent.id ~ UnpinnedEvent.createdAt ~ UnpinnedEvent.actorInterface(Actor.login), UNPINNED_EVENT)),
      // onUserBlockedEvent = Some(eventToDomain(UserBlockedEvent.id ~ UserBlockedEvent.createdAt ~ UserBlockedEvent.actorInterface(Actor.login), USER_BLOCKED_EVENT))
    )
    .map(_.toList.flatten.flatMap(_.flatten))

def issueEvents() =
  def eventToDomain[A](
      sb: SelectionBuilder[A, (String, String, Option[String])],
      `type`: IssueTimelineItemsItemType
  ): SelectionBuilder[A, GithubIssueEventOrphan] =
    sb.mapN[String, String, Option[String], GithubIssueEventOrphan]((id, createdAt, authorOpt) => issueEventToDomain(id, createdAt, authorOpt, `type`.toString()))

  IssueTimelineItemsConnection
    .nodesOption[GithubIssueEventOrphan](
      onClosedEvent = Some(eventToDomain(ClosedEvent.id ~ ClosedEvent.createdAt ~ ClosedEvent.actorInterface(Actor.login), IssueTimelineItemsItemType.CLOSED_EVENT)),
      onIssueComment = Some(eventToDomain(IssueComment.id ~ IssueComment.createdAt ~ IssueComment.authorInterface(Actor.login), IssueTimelineItemsItemType.ISSUE_COMMENT))
    )
    .map(_.toList.flatten.flatMap(_.flatten))

// scalafmt: { maxColumn = 120 }
def createPRQuery(
    repositoryId: String,
    after: Option[String]
) =
  (Query.nodeOption(repositoryId)(
    onRepository = Some(
      Repository
        .pullRequests(first = Some(50), after = after) {
          PullRequestConnection
            .nodes(pullRequestWithEvents(None, repositoryId))
            ~ PullRequestConnection
              .pageInfo {
                PageInfo.hasNextPage ~ PageInfo.endCursor
              }
              .mapN[Boolean, Option[String], GithubPageInfo](GithubPageInfo.apply)
        }
    )
  ) ~ Query
    .rateLimit() {
      (RateLimit.remaining ~ RateLimit.resetAt).mapN[Int, String, GithubRateLimit](GithubRateLimit.apply)
    })
    .map { case (prs, rateLimit) =>
      prs.flatten.map { (prs, pageInfo) =>
        (prs.toList.flatten.flatten, pageInfo)
      } -> rateLimit
    }

def issueWithEvents(cutoff: Option[String], repositoryId: String) =
  (Issue.id ~ Issue.title ~ Issue.url ~ Issue.createdAt ~ Issue
    .authorOption(onUser = Some(User.login)) ~ Issue.timelineItems(
    first = Some(100),
    itemTypes = Some(IssueTimelineItemsItemTypes),
    since = cutoff
  ) {
    issueEvents() ~ IssueTimelineItemsConnection
      .pageInfo {
        PageInfo.hasNextPage ~ PageInfo.endCursor
      }
      .mapN[Boolean, Option[String], GithubPageInfo](GithubPageInfo.apply)
  }).mapN[
    String,
    String,
    String,
    String,
    Option[
      Option[String]
    ],
    (List[GithubIssueEventOrphan], GithubPageInfo),
    (GithubIssueEventsCursor, List[GithubIssueEventOrphan])
  ] { case (id, title, url, createdAt, authorOpt, (events, eventsPage)) =>
    GithubIssueEventsCursor(
      GithubIssue(
        id,
        GithubIssueType.Issue,
        repositoryId,
        title,
        url,
        Instant.parse(createdAt),
        authorOpt.flatten,
        eventsPage.hasNextPage
      ),
      eventsPage.endCursor
    ) -> events
  }

def pullRequestWithEvents(cutoff: Option[String], repositoryId: String) =
  (PullRequest.id ~ PullRequest.title ~ PullRequest.url ~ PullRequest.createdAt ~ PullRequest
    .authorOption(onUser = Some(User.login)) ~ PullRequest.timelineItems(
    first = Some(100),
    itemTypes = Some(PullRequestTimelineItemsItemTypes),
    since = cutoff
  ) {
    pullRequestEvents() ~ PullRequestTimelineItemsConnection
      .pageInfo {
        PageInfo.hasNextPage ~ PageInfo.endCursor
      }
      .mapN[Boolean, Option[String], GithubPageInfo](GithubPageInfo.apply)
  }).mapN[
    String,
    String,
    String,
    String,
    Option[
      Option[String]
    ],
    (List[GithubIssueEventOrphan], GithubPageInfo),
    (GithubIssueEventsCursor, List[GithubIssueEventOrphan])
  ] { case (id, title, url, createdAt, authorOpt, (events, eventsPage)) =>
    GithubIssueEventsCursor(
      GithubIssue(
        id,
        GithubIssueType.PullRequest,
        repositoryId,
        title,
        url,
        Instant.parse(createdAt),
        authorOpt.flatten,
        eventsPage.hasNextPage
      ),
      eventsPage.endCursor
    ) -> events
  }

def createIssueQuery(
    repositoryId: String,
    after: Option[String]
) =
  (Query.nodeOption(repositoryId)(
    onRepository = Some(
      Repository.issues(first = Some(50), after = after) {
        IssueConnection
          .nodes(issueWithEvents(None, repositoryId))
          ~ IssueConnection
            .pageInfo(PageInfo.hasNextPage ~ PageInfo.endCursor)
            .mapN[Boolean, Option[String], GithubPageInfo](GithubPageInfo.apply)
      }
    )
  ) ~ Query
    .rateLimit() {
      (RateLimit.remaining ~ RateLimit.resetAt).mapN[Int, String, GithubRateLimit](GithubRateLimit.apply)
    })
    .map { case (issues, rateLimit) =>
      issues.flatten.map { (issues, pageInfo) =>
        (issues.toList.flatten.flatten, pageInfo)
      } -> rateLimit
    }

def createIssueEventsQuery(issueId: String, after: Option[String]) =
  (Query.nodeOption(issueId)(
    onPullRequest = Some(
      PullRequest.timelineItems(first = Some(100), itemTypes = Some(PullRequestTimelineItemsItemTypes), after = after) {
        pullRequestEvents() ~ PullRequestTimelineItemsConnection
          .pageInfo {
            PageInfo.hasNextPage ~ PageInfo.endCursor
          }
          .mapN[Boolean, Option[String], GithubPageInfo](GithubPageInfo.apply)
      }
    ),
    onIssue = Some(
      Issue.timelineItems(first = Some(100), itemTypes = Some(IssueTimelineItemsItemTypes), after = after) {
        issueEvents() ~ IssueTimelineItemsConnection
          .pageInfo {
            PageInfo.hasNextPage ~ PageInfo.endCursor
          }
          .mapN[Boolean, Option[String], GithubPageInfo](GithubPageInfo.apply)
      }
    )
  ) ~ Query
    .rateLimit() {
      (RateLimit.remaining ~ RateLimit.resetAt).mapN[Int, String, GithubRateLimit](GithubRateLimit.apply)
    })
    .map { case (events, rateLimit) =>
      events.flatten.map { (events, pageInfo) =>
        (events.toList, pageInfo)
      } -> rateLimit
    }

def searchIssuesUpdatedAfter(cutoff: Instant, repositoryId: String, repositoryName: String, after: Option[String]) =
  (Query.search(
    query = s"repo:$repositoryName updated:>${cutoff.toString}",
    `type` = SearchType.ISSUE,
    first = Some(100),
    after = after
  ) {
    (SearchResultItemConnection
      .nodesOption(
        onIssue = Some(issueWithEvents(Some(cutoff.toString), repositoryId)),
        onPullRequest = Some(pullRequestWithEvents(Some(cutoff.toString), repositoryId))
      ) ~ SearchResultItemConnection
      .pageInfo {
        PageInfo.hasNextPage ~ PageInfo.endCursor
      }
      .mapN[Boolean, Option[String], GithubPageInfo](GithubPageInfo.apply))
  } ~ Query
    .rateLimit() {
      (RateLimit.remaining ~ RateLimit.resetAt).mapN[Int, String, GithubRateLimit](GithubRateLimit.apply)
    })
    .map { case (issues, pageInfo, rateLimit) =>
      (issues.toList.flatten.flatten.flatten, pageInfo, rateLimit)
    }
