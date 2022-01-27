package lila.app
package http

import play.api.http.DefaultHttpErrorHandler
import play.api.mvc._
import play.api.mvc.Results._
import play.api.routing._
import play.api.{ Configuration, Environment, UsefulException }
import scala.concurrent.Future

import lila.common.HTTPRequest

final class ErrorHandler(
    environment: Environment,
    config: Configuration,
    router: => Router,
    mainC: => controllers.Main,
    lobbyC: => controllers.Lobby
)(implicit ec: scala.concurrent.ExecutionContext)
    extends DefaultHttpErrorHandler(environment, config, router.some) {

  override def onProdServerError(req: RequestHeader, exception: UsefulException) =
    Future {
      val actionName = HTTPRequest actionName req
      val client     = HTTPRequest clientName req
      lila.mon.http.error(actionName, client, req.method, 500).increment()
      lila.log("http").error(s"ERROR 500 $actionName", exception)
      if (canShowErrorPage(req))
        InternalServerError(views.html.site.bits.errorPage {
          lila.api.Context.error(
            req,
            lila.i18n.defaultLang,
            HTTPRequest.isSynchronousHttp(req) option lila.common.Nonce.random
          )
        })
      else InternalServerError("Sorry, something went wrong.")
    } recover { case scala.util.control.NonFatal(e) =>
      lila.log("http").error(s"""Error handler exception on "${exception.getMessage}\"""", e)
      InternalServerError("Sorry, something went very wrong.")
    }

  override def onClientError(req: RequestHeader, statusCode: Int, msg: String): Fu[Result] =
    statusCode match {
      case 404 if canShowErrorPage(req) => mainC.handlerNotFound(req)
      case 404                          => fuccess(NotFound("404 - Resource not found"))
      case 403                          => lobbyC.handleStatus(req, Results.Forbidden)
      case _ if req.attrs.contains(request.RequestAttrKey.Session) =>
        lobbyC.handleStatus(req, Results.BadRequest)
      case _ =>
        fuccess {
          Results.BadRequest("Sorry, the request could not be processed")
        }
    }

  private def canShowErrorPage(req: RequestHeader): Boolean =
    HTTPRequest.isSynchronousHttp(req) && !HTTPRequest.hasFileExtension(req)
}
