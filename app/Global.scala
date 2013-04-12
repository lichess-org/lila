package lila.app

// import lila.app.Env
import lila.hub.actorApi.monitor.AddRequest

import play.api.{ Application, GlobalSettings, Mode }
import play.api.mvc._
import play.api.mvc.Results._

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    lila.app.Env.current
  }

  override def onRouteRequest(req: RequestHeader): Option[Handler] =
    if (Env.ai.isServer) {
      if (req.path startsWith "/ai/") super.onRouteRequest(req)
      else Action(NotFound("I am an AI server")).some
    }
    else {
      Env.monitor.reporting ! AddRequest
      Env.security.wiretap(req.pp)
      Env.security.firewall.requestHandler(req).await orElse
        Env.i18n.requestHandler(req) orElse
        super.onRouteRequest(req)
    }

  override def onHandlerNotFound(req: RequestHeader): Result =
    Env.ai.isServer.fold(NotFound, controllers.Lobby.handleNotFound(req).await)

  override def onBadRequest(req: RequestHeader, error: String) = {
    BadRequest("Bad Request: " + error)
  }

  // override def onError(request: RequestHeader, ex: Throwable) =
  //   env.ai.isServer.fold(
  //     InternalServerError(ex.getMessage),
  //     Option(coreEnv).fold(Mode.Prod)(_.app.mode) match {
  //       case Mode.Prod ⇒ InternalServerError(
  //         views.html.base.errorPage(ex)(http.Context(request, none))
  //       )
  //       case _ ⇒ super.onError(request, ex)
  //     }
  //   )
}
