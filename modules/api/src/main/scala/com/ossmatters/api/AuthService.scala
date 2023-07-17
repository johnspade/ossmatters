package com.ossmatters.api

import java.time.Instant

import zio.Config.string
import zio.*
import zio.http.*
import zio.json.JsonDecoder
import zio.json.*

import com.github.api.*
import pdi.jwt.JwtAlgorithm
import pdi.jwt.JwtClaim
import pdi.jwt.JwtZIOJson

import com.ossmatters.github.crawler.GithubApiClient
import com.ossmatters.shared.GithubConfig

trait AuthService:
  def githubAuthUrl: String
  def authenticate(code: String): ZIO[Any, Throwable, String]
  def generateJwt(login: String): ZIO[Any, Throwable, String]
  def validateJwt(jwt: String): ZIO[Any, Nothing, Boolean]
  def cookieDuration: Duration

final class AuthServiceLive(
    httpClient: Client,
    clientId: String,
    clientSecret: String,
    githubClient: GithubApiClient,
    jwtSecret: String,
    githubConfig: GithubConfig,
    redirectUri: String
) extends AuthService:
  override val cookieDuration: Duration = Duration.fromSeconds(60 * 60 * 24 * 7)

  override val githubAuthUrl: String =
    s"https://github.com/login/oauth/authorize?client_id=$clientId&redirect_uri=$redirectUri&scope=read:org"

  override def authenticate(code: String): ZIO[Any, Throwable, String] =
    exchangeCodeForToken(code)

  override def generateJwt(login: String): ZIO[Any, Throwable, String] =
    for
      now <- Clock.instant
      claim = JwtClaim(
        issuer = Some("OSSMatters"),
        subject = Some(login),
        issuedAt = Some(now.getEpochSecond),
        expiration = Some(now.plusSeconds(cookieDuration.getSeconds()).getEpochSecond)
      )
      jwt <- ZIO.attemptBlocking(JwtZIOJson.encode(claim, jwtSecret, JwtAlgorithm.HS256))
    yield jwt

  override def validateJwt(jwt: String): ZIO[Any, Nothing, Boolean] =
    ZIO.attemptBlocking(JwtZIOJson.validate(jwt, jwtSecret, Seq(JwtAlgorithm.HS256))).isSuccess

  private def exchangeCodeForToken(code: String): ZIO[Any, Throwable, String] =
    ZIO.scoped(
      for
        response <- httpClient
          .url(URL.decode("https://github.com/login/oauth/access_token").toOption.get)
          .addHeader("Accept", "application/json")
          .post("")(
            Body.fromURLEncodedForm(
              Form.fromStrings("client_id" -> clientId, "code" -> code, "client_secret" -> clientSecret)
            )
          )
        responseString <- response.body.asString
        accessTokenResponse = responseString.fromJson[AccessTokenResponse]
        accessToken <- accessTokenResponse
          .map(_.access_token)
          .fold(
            error => ZIO.fail(new Exception(s"Failed to parse access token response: $error")),
            ZIO.succeed
          )
        login <- githubClient.execute(userQuery, accessToken)
        user <-
          if githubConfig.usernames.toList.contains(login) then ZIO.succeed(login)
          else ZIO.fail(new Exception("User not found"))
      yield user
    )

  private final case class AccessTokenResponse(access_token: String, token_type: String, scope: String)
      derives JsonDecoder

  private val userQuery = Query.viewer(User.login)
end AuthServiceLive

object AuthServiceLive:
  val layer = ZLayer(
    for
      httpClient   <- ZIO.service[Client]
      clientId     <- ZIO.config(string("CLIENT_ID"))
      clientSecret <- ZIO.config(string("CLIENT_SECRET"))
      jwtSecret    <- ZIO.config(string("JWT_SECRET"))
      githubClient <- ZIO.service[GithubApiClient]
      githubConfig <- ZIO.service[GithubConfig]
      redirectUri  <- ZIO.config(string("AUTH_REDIRECT_URI"))
    yield AuthServiceLive(httpClient, clientId, clientSecret, githubClient, jwtSecret, githubConfig, redirectUri)
  )
