package controllers

import play.api.mvc._
import scala.concurrent.duration._

import lila.app._
import lila.common.HTTPRequest.{ isApi, isLocalApp }
import lila.common.ResponseHeaders.{ allowMethods, headersFor }

object Options extends LilaController {

  val root = all("")

  def all(url: String) = Action { req =>
    if (isLocalApp(req) || isApi(req).pp) {
      NoContent.withHeaders({
        headersFor(req) ::: List(
          "Allow" -> allowMethods,
          "Access-Control-Max-Age" -> "1728000"
        )
      }: _*)
    } else
      NotFound
  }
}
