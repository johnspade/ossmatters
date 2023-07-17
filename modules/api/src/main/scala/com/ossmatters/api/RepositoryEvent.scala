package com.ossmatters.api

import java.time.Instant

final case class RepositoryEvent(
    issueId: String,
    isPr: Boolean,
    issueTitle: String,
    issueUrl: String,
    issueCreatedAt: Instant,
    issueAuthor: Option[String],
    repository: String,
    eventType: Option[String],
    eventAuthor: Option[String],
    eventCreatedAt: Option[Instant]
)
