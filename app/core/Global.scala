package lila
package core

import play.api.{ Application, GlobalSettings }
import play.api.mvc._
import play.api.mvc.Results._

object Global extends GlobalSettings {

  private[this] var coreEnv: CoreEnv = _

  def env = Option(coreEnv) err "The environment is not ready"

  override def onStart(app: Application) {

    coreEnv = CoreEnv(app)

    if (env.ai.isServer) println("Running as AI server")
    else core.Cron start env
  }

  override def onRouteRequest(req: RequestHeader): Option[Handler] = {
    env.monitor.rpsProvider.countRequest()
    env.i18n.requestHandler(req) orElse super.onRouteRequest(req)
  }

  override def onHandlerNotFound(req: RequestHeader): Result = {
    controllers.Lobby handleNotFound req 
  }

  override def onBadRequest(req: RequestHeader, error: String) = {
    BadRequest("Bad Request: " + error)
  }
}
