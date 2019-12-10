package lila.app
package http

import play.api.http.DefaultHttpErrorHandler
import play.api.mvc._
import play.api.mvc.Results._
import play.api.routing._
import play.api.{ Configuration, Environment, UsefulException }
import play.core.SourceMapper
import scala.concurrent.Future

import lila.common.HTTPRequest

final class ErrorHandler(
    environment: Environment,
    config: Configuration,
    sourceMapper: Option[SourceMapper],
    router: => Option[Router],
    mainC: => controllers.Main,
    lobbyC: => controllers.Lobby
) extends DefaultHttpErrorHandler(environment, config, sourceMapper, router) {

  override def onProdServerError(req: RequestHeader, exception: UsefulException) = Future {
    val actionName = HTTPRequest actionName req
    val apiVersion = lila.api.Mobile.Api.requestVersion(req)
    lila.mon.http.error(actionName, apiVersion, 500)
    lila.log("http").error(s"ERROR 500 $actionName", exception)
    if (canShowErrorPage(req))
      InternalServerError(views.html.base.errorPage(exception) {
        lila.api.Context.error(
          req,
          lila.i18n.defaultLang,
          HTTPRequest.isSynchronousHttp(req) option lila.common.Nonce.random
        )
      })
    else InternalServerError(exception.getMessage)
  } recover {
    case util.control.NonFatal(e) =>
      lila.log("http").error(s"""Error handler exception on "${exception.getMessage}\"""", e)
      InternalServerError("Sorry, something went very wrong.")
  }

  override def onClientError(req: RequestHeader, statusCode: Int, msg: String): Fu[Result] =
    statusCode match {
      case 404 if canShowErrorPage(req) => mainC.handlerNotFound(req)
      case 404 => fuccess(NotFound("404 - Resource not found"))
      case 403 => lobbyC.handleStatus(req, Results.Forbidden)
      case _ =>
        lobbyC.handleStatus(req, Results.BadRequest)
    }

  private def canShowErrorPage(req: RequestHeader): Boolean =
    HTTPRequest.isSynchronousHttp(req) && !HTTPRequest.hasFileExtension(req)
}
