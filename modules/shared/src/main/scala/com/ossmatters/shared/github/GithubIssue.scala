package com.ossmatters.shared.github

import java.time.Instant

final case class GithubIssue(
    id: String,
    `type`: GithubIssueType,
    repositoryId: String,
    title: String,
    url: String,
    createdAt: Instant,
    author: Option[String],
    hasMoreEvents: Boolean
)
