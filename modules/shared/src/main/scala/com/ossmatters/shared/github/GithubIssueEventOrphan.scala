package com.ossmatters.shared.github

import java.time.Instant

final case class GithubIssueEventOrphan(
    id: String,
    `type`: String,
    createdAt: Instant,
    author: Option[String]
)
