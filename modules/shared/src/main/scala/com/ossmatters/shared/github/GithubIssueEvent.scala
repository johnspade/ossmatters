package com.ossmatters.shared.github

import java.time.Instant

final case class GithubIssueEvent(
    id: String,
    issueId: String,
    `type`: String,
    createdAt: Instant,
    author: Option[String]
)
