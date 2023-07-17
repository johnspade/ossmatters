package com.ossmatters.template

import java.time.LocalDate

import zio.http.htmx
import zio.http.template.Dom.attr
import zio.http.template.*

import cats.data.NonEmptyList

import com.ossmatters.api.OssEvent

object People extends htmx.Attributes:
  def template(
      users: NonEmptyList[String],
      events: Map[String, List[OssEvent]],
      selected: String,
      start: Option[LocalDate] = None,
      end: Option[LocalDate] = None,
      formValidationError: Option[String] = None
  ): Html =
    div(
      idAttr := "people",
      css    := "flex flex-1 flex-col gap-4 p-4 md:gap-8 md:p-6",
      h1(
        css := "text-4xl bold",
        "People"
      ),
      form(
        hxPostAttr   := "/people",
        hxSwapAttr   := "innerHTML",
        hxTargetAttr := "#main",
        css          := "flex gap-4 items-center",
        div(
          css := "flex gap-4 items-center",
          label(
            css     := "sr-only",
            forAttr := "start-date",
            "Start Date"
          ),
          input(
            id              := "start-date",
            nameAttr        := "start-date",
            typeAttr        := "date",
            css             := "border border-gray-300 rounded-md p-2",
            placeholderAttr := "Start Date",
            requiredAttr    := "true",
            valueAttr       := start.map(_.toString).getOrElse("")
          )
        ),
        "-",
        div(
          css := "flex gap-4 items-center",
          label(
            css     := "sr-only",
            forAttr := "end-date",
            "End Date"
          ),
          input(
            id              := "end-date",
            nameAttr        := "end-date",
            typeAttr        := "date",
            css             := "border border-gray-300 rounded-md p-2",
            placeholderAttr := "End Date",
            requiredAttr    := "true",
            valueAttr       := end.map(_.toString).getOrElse("")
          )
        ),
        div(
          css := "flex gap-4 items-center",
          label(
            css     := "sr-only",
            forAttr := "username",
            "Username"
          ),
          select(
            id           := "username",
            nameAttr     := "username",
            css          := "border border-gray-300 rounded-md p-2",
            requiredAttr := "true",
            users.toList.map: user =>
              if user == selected then
                option(
                  valueAttr    := user,
                  selectedAttr := "selected",
                  user
                )
              else
                option(
                  valueAttr := user,
                  user
                )
          )
        ),
        button(
          css      := "bg-gray-500 hover:bg-gray-700 text-white font-bold py-2 px-4 rounded",
          typeAttr := "submit",
          "Submit"
        )
      ),
      div(
        css := "text-red-500",
        formValidationError.getOrElse("")
      ),
      div(
        css := "prose prose-sm max-w-none",
        events.toList
          .sortBy(_._1)
          .map: (repo, events) =>
            div(
              h1(repo),
              ul(
                events.toList.map(_.toHtml()): _*
              )
            )
          .toSeq
      )
    )
