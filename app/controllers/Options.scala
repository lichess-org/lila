package controllers

import play.api.mvc._
import scala.concurrent.duration._

import lila.app._
import lila.common.HTTPRequest.{ isApi, isLocalhost8080, localhost8080 }

object Options extends LilaController {

  val root = all("")

  def all(url: String) = Action { req =>
    val isLocalhost = isLocalhost8080(req)
    if (isLocalhost || isApi(req).pp) {
      val methods = List("GET", "POST").filter { m =>
        router.handlerFor(req.copy(method = m)).isDefined
      }
      if (methods.nonEmpty) {
        val allow = ("OPTIONS" :: methods) mkString ", "
        NoContent.withHeaders(
          List(
            "Allow" -> allow,
            "Access-Control-Allow-Methods" -> allow,
            "Access-Control-Allow-Origin" -> { if (isLocalhost) localhost8080 else "*" },
            "Access-Control-Allow-Headers" -> {
              List(
                "Origin", "Authorization", "If-Modified-Since", "Cache-Control"
              ) ::: isLocalhost.??(List("X-Requested-With", "sessionId"))
            }.mkString(", "),
            "Access-Control-Max-Age" -> "1728000",
            "Vary" -> "Origin"
          ): _*
        )
      } else
        NotFound
    } else
      NotFound
  }

  private lazy val router = lila.common.PlayApp.router
}
