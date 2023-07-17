package com.ossmatters.github.crawler

import com.ossmatters.shared.github.GithubIssue

final case class GithubIssueEventsCursor(issue: GithubIssue, cursor: Option[String])
