package com.ossmatters.api

import java.time.Instant
import java.time.ZoneId

import zio.http.template.*

import cats.data.NonEmptyList

case class OssEvent(
    dateTime: Instant,
    isPr: Boolean,
    author: String,
    types: NonEmptyList[String],
    url: String,
    title: String
) {
  def issueTypeString: String = if isPr then "a PR" else "an issue"

  def toHtml(): Html = li(
    b(author),
    s" ${types.map(TypeDescription).toList.mkString(", ")} $issueTypeString: ",
    a(href := url, targetAttr := "_blank", title),
    i(s" on ${dateFormatter.format(dateTime.atZone(UTC))}")
  )

  private val TypeDescription = Map(
    "MERGED_EVENT"        -> "merged",
    "CLOSED_EVENT"        -> "closed",
    "PULL_REQUEST_REVIEW" -> "reviewed",
    "ISSUE_COMMENT"       -> "commented on",
    "PULL_REQUEST_COMMIT" -> "pushed to",
    "CREATED_EVENT"       -> "created"
  )
}

private val dateFormatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
