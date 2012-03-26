package lila.http

import play.api._
import play.api.mvc._
import play.api.mvc.Results._

import lila.system.SystemEnv

object Global extends GlobalSettings {

  private[this] var systemEnv: SystemEnv = _

  def env = Option(systemEnv) err "The environment is not ready"

  override def onStart(app: Application) {
    systemEnv = new SystemEnv(app.configuration.underlying)
    new Cron(systemEnv)(app)
  }

  override def onHandlerNotFound(request: RequestHeader): Result = {
    NotFound("Not found " + request)
  }

  override def onBadRequest(request: RequestHeader, error: String) = {
    BadRequest("Bad Request: " + error)
  }

  //override def onError(request: RequestHeader, ex: Throwable): Result =
    //InternalServerError(ex.getMessage)
}
