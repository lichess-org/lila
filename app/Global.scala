package lila

import play.api.{ Application, GlobalSettings }
import play.api.mvc._
import play.api.mvc.Results._

object Global extends GlobalSettings {

  private[this] var systemEnv: SystemEnv = _

  def env = Option(systemEnv) err "The environment is not ready"

  override def onStart(app: Application) {
    val settings = new Settings(app.configuration.underlying)
    systemEnv = new SystemEnv(app, settings)

    if (env.isAiServer) println("Running as AI server")
    else Cron start env
  }

  override def onHandlerNotFound(request: RequestHeader): Result = {
    NotFound("Not found " + request)
  }

  override def onBadRequest(request: RequestHeader, error: String) = {
    BadRequest("Bad Request: " + error)
  }

  //override def onError(request: RequestHeader, e: Throwable): Result =
    //InternalServerError(e.getMessage)
}
