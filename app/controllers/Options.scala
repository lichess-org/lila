package controllers

import play.api.mvc._
import scala.concurrent.duration._

import lila.app._
import lila.common.HTTPRequest.isApiOrLocalApp
import lila.common.ResponseHeaders.allowMethods

object Options extends LilaController {

  val root = all("")

  def all(url: String) = Action { req =>
    if (isApiOrLocalApp(req)) NoContent.withHeaders(
      "Allow" -> allowMethods,
      "Access-Control-Max-Age" -> "1728000"
    )
    else NotFound
  }
}
