package lila.app

import lila.common.HTTPRequest
import play.api.mvc._
import play.api.mvc.Results._
import play.api.{ Application, GlobalSettings, Mode }

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    kamon.Kamon.start()
    lila.app.Env.current
  }

  override def onStop(app: Application) {
    kamon.Kamon.shutdown()
  }

  override def onRouteRequest(req: RequestHeader): Option[Handler] = {
    lila.mon.http.request.all()
    if (req.remoteAddress contains ":") lila.mon.http.request.ipv6()
    Env.i18n.requestHandler(req) orElse super.onRouteRequest(req)
  }

  private def niceError(req: RequestHeader): Boolean =
    req.method == "GET" &&
      HTTPRequest.isSynchronousHttp(req) &&
      !HTTPRequest.hasFileExtension(req)

  override def onHandlerNotFound(req: RequestHeader) =
    if (niceError(req)) controllers.Main.notFound(req)
    else fuccess(NotFound("404 - Resource not found"))

  override def onBadRequest(req: RequestHeader, error: String) =
    if (error startsWith "Illegal character in path") fuccess(Redirect("/"))
    else if (error startsWith "Cannot parse parameter") onHandlerNotFound(req)
    else if (niceError(req)) {
      lila.mon.http.response.code400()
      controllers.Lobby.handleStatus(req, Results.BadRequest)
    }
    else fuccess(BadRequest(error))

  override def onError(req: RequestHeader, ex: Throwable) =
    if (niceError(req)) {
      if (lila.common.PlayApp.isProd) {
        lila.mon.http.response.code500()
        fuccess(InternalServerError(views.html.base.errorPage(ex)(lila.api.Context(req))))
      }
      else super.onError(req, ex)
    }
    else fuccess(InternalServerError(ex.getMessage))
}
