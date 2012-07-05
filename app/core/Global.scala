package lila
package core

import play.api.{ Application, GlobalSettings, Mode }
import play.api.mvc._
import play.api.mvc.Results._

object Global extends GlobalSettings {

  private[this] var coreEnv: CoreEnv = _

  def env = Option(coreEnv) err "The environment is not ready"

  override def onStart(app: Application) {

    coreEnv = CoreEnv(app)

    println("Configured as " + env.configName)

    if (env.ai.isServer) println("Running as AI server")
    else if (app.mode == Mode.Test) println("Running without cron")
    else core.Cron start env
  }

  override def onRouteRequest(req: RequestHeader): Option[Handler] = 
    if (env.ai.isServer) super.onRouteRequest(req)
    else {
      env.monitor.rpsProvider.countRequest()
      env.security.firewall.requestHandler(req) orElse
      env.i18n.requestHandler(req) orElse 
      super.onRouteRequest(req)
    }

  override def onHandlerNotFound(req: RequestHeader): Result = {
    controllers.Lobby handleNotFound req
  }

  override def onBadRequest(req: RequestHeader, error: String) = {
    BadRequest("Bad Request: " + error)
  }

  override def onError(request: RequestHeader, ex: Throwable) =
    Option(coreEnv).fold(_.app.mode, Mode.Prod) match {
      case Mode.Prod ⇒ InternalServerError(
        views.html.base.errorPage(ex)(http.Context(request, none))
      )
      case _ ⇒ super.onError(request, ex)
    }
}
