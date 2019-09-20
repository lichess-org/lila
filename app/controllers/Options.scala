package controllers

import play.api.mvc._
import scala.concurrent.duration._

import lila.app._
import lila.common.HTTPRequest.{ isApi, isLocalApp, apiHeaders }

object Options extends LilaController {

  val root = all("")

  def all(url: String) = Action { req =>
    if (isLocalApp(req) || isApi(req).pp) {
      val methods = List("GET", "POST").filter { m =>
        router.handlerFor(req.copy(method = m)).isDefined
      }
      if (methods.nonEmpty) {
        val allow = ("OPTIONS" :: methods) mkString ", "
        NoContent.withHeaders({
          apiHeaders(req) ::: List(
            "Allow" -> allow,
            "Access-Control-Allow-Methods" -> allow,
            "Access-Control-Allow-Headers" -> {
              List(
                "Origin", "Authorization", "If-Modified-Since", "Cache-Control"
              ) ::: isLocalApp(req).??(List("X-Requested-With", "sessionId", "Content-Type"))
            }.mkString(", "),
            "Access-Control-Max-Age" -> "1728000",
            "Access-Control-Allow-Credentials" -> "true"
          )
        }: _*)
      } else
        NotFound
    } else
      NotFound
  }

  private lazy val router = lila.common.PlayApp.router
}
