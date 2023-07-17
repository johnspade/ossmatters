package com.ossmatters.api

import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import scala.util.Try

import zio.*
import zio.http.Cookie.SameSite
import zio.http.*
import zio.http.template.Html
import zio.json.*

import com.ossmatters.shared.GithubConfig
import com.ossmatters.template.*

final class Routes(authService: AuthService, dashboardService: DashboardService, githubConfig: GithubConfig):
  val loginApp = zio.http
    .Routes(
      Method.GET / "login" -> handler {
        Response.html(Login.template)
      },
      Method.POST / "sign-in" -> handler(
        Response.ok.addHeader("HX-Redirect", authService.githubAuthUrl)
      ),
      Method.GET / "callback" -> handler { (req: Request) =>
        val code = req.url.queryParams.getAll("code").head // todo
        for
          user <- authService.authenticate(code)
          jwt  <- authService.generateJwt(user)
        yield Response
          .html(Main.redirectToRoot)
          .addCookie(
            Cookie.Response(
              "jwt",
              jwt,
              isHttpOnly = true,
              isSecure = true,
              sameSite = Some(SameSite.Strict),
              maxAge = Some(authService.cookieDuration)
            )
          )
      }
    )
    .handleError(e => Response.internalServerError(e.getMessage()))
    .toHttpApp

  val secureApp = zio.http
    .Routes(
      Method.POST / "logout" -> handler { (req: Request) =>
        Response.ok
          .addHeader("HX-Redirect", "/login")
          .addCookie(
            Cookie.Response(
              "jwt",
              "",
              isHttpOnly = true,
              isSecure = true,
              sameSite = Some(SameSite.Strict),
              maxAge = Some(Duration.fromMillis(-1L))
            )
          )
      },
      Method.GET / "" -> handler(
        dashboardService
          .getReport(None, None)
          .map: report =>
            Response.html(Main.template(Projects.template(report)))
      ),
      Method.GET / "projects" -> handler { (req: Request) =>
        dashboardService
          .getReport(None, None)
          .map: report =>
            htmxAwareTemplate(req, Projects.template(report))
      },
      Method.GET / "people" -> handler { (req: Request) =>
        dashboardService
          .getUserReport(None, None, githubConfig.usernames.head)
          .map: report =>
            htmxAwareTemplate(
              req,
              People.template(githubConfig.usernames, report, githubConfig.usernames.head)
            )
      },
      Method.GET / "settings" -> handler { (req: Request) =>
        htmxAwareTemplate(req, Settings.template(githubConfig.repos, githubConfig.usernames))
      },
      Method.POST / "projects" -> handler { (req: Request) =>
        for
          form <- req.body.asURLEncodedForm
          startDateStr = form.get("start-date").flatMap(_.stringValue)
          endDateStr   = form.get("end-date").flatMap(_.stringValue)
          validation   = validateDates(startDateStr, endDateStr)
          html <- validation match
            case Right((startDateOpt, endDateOpt)) =>
              dashboardService
                .getReport(startDateOpt, endDateOpt)
                .map: report =>
                  Projects.template(report, startDateOpt, endDateOpt)
            case Left(errorMessage) =>
              ZIO.succeed(
                Projects.template(
                  events = Map.empty,
                  start = None,
                  end = None,
                  formValidationError = Some(errorMessage)
                )
              )
        yield Response.html(html)
      },
      Method.POST / "people" -> handler { (req: Request) =>
        for
          form <- req.body.asURLEncodedForm
          startDateStr = form.get("start-date").flatMap(_.stringValue)
          endDateStr   = form.get("end-date").flatMap(_.stringValue)
          validation   = validateDates(startDateStr, endDateStr)
          user = form
            .get("username")
            .flatMap(_.stringValue)
            .getOrElse(githubConfig.usernames.head)
          html <- validation match
            case Right((startDateOpt, endDateOpt)) =>
              dashboardService
                .getUserReport(startDateOpt, endDateOpt, user)
                .map: report =>
                  People.template(githubConfig.usernames, report, user, startDateOpt, endDateOpt)
            case Left(errorMessage) =>
              ZIO.succeed(
                People.template(
                  users = githubConfig.usernames,
                  events = Map.empty,
                  selected = user,
                  start = None,
                  end = None,
                  formValidationError = Some(errorMessage)
                )
              )
        yield Response.html(html)
      }
    )
    .handleError(e => Response.internalServerError(e.getMessage()))
    .toHttpApp @@ Middleware.customAuthZIO[Any](
    verify = req =>
      req
        .cookieWithZIO("jwt")(cookie => authService.validateJwt(cookie.content))
        .mapError[Response](e => Response.redirect(URL.root / "login"))
  )

  private def validateDates(
      startDateStr: Option[String],
      endDateStr: Option[String]
  ): Either[String, (Option[LocalDate], Option[LocalDate])] = {
    (for
      start     <- startDateStr.toRight("Start date is required.")
      end       <- endDateStr.toRight("End date is required.")
      startDate <- Try(LocalDate.parse(start)).toOption.toRight("Invalid start date format.")
      endDate   <- Try(LocalDate.parse(end)).toOption.toRight("Invalid end date format.")
    yield (startDate, endDate)) match {
      case Right((startDate, endDate)) if startDate.isAfter(endDate) =>
        Left("Start date must be before end date.")
      case Right((startDate, endDate)) if DAYS.between(startDate, endDate) > 31 =>
        Left("The interval between start and end date must not be more than 31 days.")
      case Right((startDate, endDate)) =>
        Right((Some(startDate), Some(endDate)))
      case Left(errorMessage) =>
        Left(errorMessage)
    }
  }

  private def htmxAwareTemplate(request: Request, template: Html) =
    if request.headers.get("hx-request").contains("true") then Response.html(template)
    else Response.html(Main.template(template))

object Routes:
  val layer = ZLayer.fromFunction(Routes(_, _, _))
