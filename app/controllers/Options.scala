package controllers

import com.github.ghik.silencer.silent

import lila.app._
import lila.app.http.ResponseHeaders.allowMethods
import lila.common.HTTPRequest.isApiOrApp

final class Options(env: Env) extends LilaController(env) {

  val root = all("")

  def all(@silent url: String) = Action { req =>
    if (isApiOrApp(req)) NoContent.withHeaders(
      "Allow" -> allowMethods,
      "Access-Control-Max-Age" -> "1728000"
    )
    else NotFound
  }
}
