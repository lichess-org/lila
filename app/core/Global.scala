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

    //if (env.isAiServer) println("Running as AI server")
    //else Cron start env
  }

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    println(request)
    env.i18n.requestHandler(request) orElse super.onRouteRequest(request)
  }

  override def onHandlerNotFound(request: RequestHeader): Result = {
    NotFound("Not found " + request)
  }

  override def onBadRequest(request: RequestHeader, error: String) = {
    BadRequest("Bad Request: " + error)
  }
}
