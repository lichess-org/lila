package lila.app

import play.api.mvc._
import play.api.mvc.Results._
import play.api.{ Application, GlobalSettings, Mode }

import lila.hub.actorApi.monitor.AddRequest

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    lila.app.Env.current
  }

  override def onRouteRequest(req: RequestHeader): Option[Handler] =
    if (req.path startsWith "/ai/") super.onRouteRequest(req)
    else if (Env.ai.ServerOnly) {
      Action(NotFound("I am an AI server")).some
    }
    else {
      Env.monitor.reporting ! AddRequest
      // Env.security.wiretap(req)
      // Env.security.firewall.requestHandler(req).await orElse
      Env.i18n.requestHandler(req) orElse
        super.onRouteRequest(req)
    }

  private def niceError(req: RequestHeader): Boolean = req.method == "GET" && {
    Env.ai.ServerOnly || (lila.common.HTTPRequest isSynchronousHttp req)
  }

  override def onHandlerNotFound(req: RequestHeader) =
    if (niceError(req)) controllers.Lobby.handleStatus(req, Results.NotFound)
    else fuccess(NotFound)

  override def onBadRequest(req: RequestHeader, error: String) =
    if (niceError(req)) {
      logwarn("[global] bad request: " + error)
      controllers.Lobby.handleStatus(req, Results.BadRequest)
    }
    else fuccess(BadRequest(error))

  override def onError(req: RequestHeader, ex: Throwable) =
    if (niceError(req)) {
      if (lila.common.PlayApp.isProd)
        fuccess(InternalServerError(views.html.base.errorPage(ex)(lila.api.Context(req))))
      else super.onError(req, ex)
    }
    else fuccess(InternalServerError(ex.getMessage))
}
