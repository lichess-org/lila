package controllers

import com.github.ghik.silencer.silent
import play.api.mvc._

import lila.app._
import lila.app.http.ResponseHeaders.allowMethods
import lila.common.HTTPRequest.isApiOrApp

final class Options(cc: ControllerComponents) extends AbstractController(cc) {

  val root = all("")

  def all(@silent url: String) = Action { req =>
    if (isApiOrApp(req))
      NoContent.withHeaders(
        "Allow"                  -> allowMethods,
        "Access-Control-Max-Age" -> "1728000"
      )
    else NotFound
  }
}
