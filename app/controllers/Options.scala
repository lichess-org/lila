package controllers

import lila.app._
import lila.common.HTTPRequest.isApiOrApp
import lila.app.http.ResponseHeaders.allowMethods

final class Options(env: Env) extends LilaController(env) {

  val root = all("")

  def all(url: String) = Action { req =>
    if (isApiOrApp(req)) NoContent.withHeaders(
      "Allow" -> allowMethods,
      "Access-Control-Max-Age" -> "1728000"
    )
    else NotFound
  }
}
