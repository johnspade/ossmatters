package com.ossmatters.shared.github

import java.time.Instant

final case class GithubRepository(id: String, name: String, owner: String, fetchState: FetchState, syncCutoff: Instant):
  val fullName: String = s"$owner/$name"
