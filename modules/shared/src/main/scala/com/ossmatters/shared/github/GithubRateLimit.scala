package com.ossmatters.shared.github

final case class GithubRateLimit(remaining: Int, resetAt: String)
