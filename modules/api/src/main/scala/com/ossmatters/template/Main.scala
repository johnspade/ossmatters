package com.ossmatters.template

import zio.http.htmx
import zio.http.template.Dom.attr
import zio.http.template.*

import cats.data.NonEmptyList

import com.ossmatters.api.OssEvent

object Main extends htmx.Attributes:
  def template(content: Html): Html =
    html(
      head(
        meta(charsetAttr := "utf-8"),
        title("OSS Matters"),
        tailwind,
        link(
          relAttr  := "stylesheet",
          hrefAttr := "https://unpkg.com/@tailwindcss/typography@0.5.0/dist/typography.min.css"
        ),
        htmxScript
      ),
      body(
        header(
          css := "flex h-20 w-full items-center px-4",
          h1(
            css := "text-4xl bold uppercase text-gray-700 tracking-wide text-center md:text-left",
            "OSS Matters"
          ),
          nav(
            Dom.attr("aria-label", "Main"),
            Dom.attr("data-orientation", "horizontal"),
            dirAttr := "ltr",
            css     := "relative z-10 max-w-max flex-1 items-center justify-center hidden lg:flex"
          ),
          button(
            css        := "ml-auto bg-red-500 hover:bg-red-700 text-white font-bold py-2 px-4 rounded",
            hxPostAttr := "/logout",
            "Logout"
          )
        ),
        div(
          css := "grid min-h-screen w-full lg:grid-cols-[280px_1fr]",
          div(
            css := "hidden border-r bg-gray-100/40 lg:block",
            div(
              css := "flex h-full max-h-screen flex-col gap-2",
              nav(
                css          := "grid items-start px-4 text-sm font-medium",
                hxTargetAttr := "#main",
                a(
                  css := "flex w-full items-center py-2 text-lg font-semibold transition-colors hover:bg-gray-100 hover:text-gray-900 focus:bg-gray-100 focus:text-gray-900 focus:outline-none disabled:pointer-events-none disabled:opacity-50 data-[active]:bg-gray-100/50 data-[state=open]:bg-gray-100/50",
                  href          := "/projects",
                  hxGetAttr     := "/projects",
                  hxPushUrlAttr := "true",
                  "Projects"
                ),
                a(
                  css := "flex w-full items-center py-2 text-lg font-semibold transition-colors hover:bg-gray-100 hover:text-gray-900 focus:bg-gray-100 focus:text-gray-900 focus:outline-none disabled:pointer-events-none disabled:opacity-50 data-[active]:bg-gray-100/50 data-[state=open]:bg-gray-100/50",
                  href          := "/people",
                  hxGetAttr     := "/people",
                  hxPushUrlAttr := "true",
                  "People"
                ),
                a(
                  css := "flex w-full items-center py-2 text-lg font-semibold transition-colors hover:bg-gray-100 hover:text-gray-900 focus:bg-gray-100 focus:text-gray-900 focus:outline-none disabled:pointer-events-none disabled:opacity-50 data-[active]:bg-gray-100/50 data-[state=open]:bg-gray-100/50",
                  href          := "/settings",
                  hxGetAttr     := "/settings",
                  hxPushUrlAttr := "true",
                  "Settings"
                )
              )
            )
          ),
          div(
            css := "flex flex-col",
            main(
              idAttr := "main",
              content
            )
          )
        ),
        footer(
          css := "flex flex-col gap-2 sm:flex-row py-6 w-full shrink-0 items-center px-4 md:px-6 border-t",
          p(
            css := "text-xs text-gray-500",
            "© 2024 Ivan L."
          ),
          nav(
            css := "sm:ml-auto flex gap-4 sm:gap-6",
            a(
              css  := "text-xs hover:underline underline-offset-4",
              href := "https://www.linkedin.com/in/ivan-l-dev/",
              "LinkedIn"
            )
            // ... Footer links
          )
        )
      )
    )

  val tailwind = script(
    srcAttr := "https://cdn.tailwindcss.com"
  )
  val htmxScript = script(
    srcAttr := "https://unpkg.com/htmx.org@1.9.10",
    attr("integrity", "sha384-D1Kt99CQMDuVetoL1lrYwg5t+9QdHe7NLX/SoJYkXDFfX37iInKRy5xLSi8nO7UC"),
    attr("crossorigin", "anonymous")
  )
  val redirectToRoot = html(
    head(meta(httpEquivAttr := "refresh", contentAttr := "0; url='/'")),
    body()
  )
end Main
