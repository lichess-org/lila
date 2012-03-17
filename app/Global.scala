package lila.http

import play.api._
import play.api.mvc._
import play.api.mvc.Results._

object Global extends GlobalSettings {

  override def onHandlerNotFound(request: RequestHeader): Result = {
    NotFound("Not found " + request)
  }

  override def onBadRequest(request: RequestHeader, error: String) = {
    BadRequest("Bad Request: " + error)
  }

  override def onError(request: RequestHeader, ex: Throwable): Result =
    InternalServerError(ex.getMessage)
}
