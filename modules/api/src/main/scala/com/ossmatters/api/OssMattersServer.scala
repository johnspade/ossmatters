package com.ossmatters.api

import zio.*
import zio.http.*
import zio.stream.ZStream

final class OssMattersServer(routes: com.ossmatters.api.Routes):
  private val app = routes.loginApp ++ routes.secureApp

  def start: ZIO[Any, Throwable, Unit] =
    for {
      port <- System.envOrElse("PORT", "8080").map(_.toInt)
      _    <- Server.serve(app).provide(Server.defaultWithPort(port))
    } yield ()

object OssMattersServer:
  val layer = ZLayer.fromFunction(OssMattersServer(_))
