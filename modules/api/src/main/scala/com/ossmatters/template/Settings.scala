package com.ossmatters.template

import zio.http.template.*

import cats.data.NonEmptyList

import com.ossmatters.shared.github.RepositoryName

object Settings:
  def template(repos: NonEmptyList[RepositoryName], usernames: NonEmptyList[String]): Html =
    div(
      idAttr := "settings",
      css    := "flex flex-1 flex-col gap-4 p-4 md:gap-8 md:p-6",
      h1(
        css := "text-4xl bold",
        "Settings"
      ),
      div(
        css := "prose prose-sm max-w-none",
        h1("Projects"),
        ul(
          repos
            .map: repo =>
              li(
                a(
                  href := s"https://github.com/${repo.owner}/${repo.name}",
                  s"${repo.owner}/${repo.name}"
                )
              )
            .toList
        ),
        h1("People"),
        ul(
          usernames
            .map: username =>
              li(
                a(
                  href := s"https://github.com/$username",
                  username
                )
              )
            .toList
        )
      )
    )
