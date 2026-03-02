package lila.app
package http

import play.api.http.DefaultHttpErrorHandler
import play.api.mvc.*
import play.api.mvc.Results.*
import play.api.routing.*
import play.api.{ Configuration, Environment, UsefulException }

import lila.api.{ LoginContext, PageContext }
import lila.common.HTTPRequest

final class ErrorHandler(
    environment: Environment,
    config: Configuration,
    router: => Router,
    mainC: => controllers.Main,
    lobbyC: => controllers.Lobby
)(using Executor)
    extends DefaultHttpErrorHandler(environment, config, router.some)
    with lila.web.ResponseWriter:

  override def onProdServerError(req: RequestHeader, exception: UsefulException) =
    Future {
      val actionName = HTTPRequest.actionName(req)
      val client = HTTPRequest.clientName(req)
      lila.mon.http.errorCount(actionName, client, req.method, 500).increment()
      lila.log("http").error(s"ERROR 500 $actionName", exception)
      if canShowErrorPage(req) then
        given PageContext = PageContext(
          lila.api.Context(req, lila.core.i18n.defaultLang, LoginContext.anon, lila.pref.Pref.default),
          lila.api.PageData.error(HTTPRequest.isSynchronousHttp(req).option(lila.ui.Nonce.random))
        )
        InternalServerError(views.base.page(views.site.ui.errorPage))
      else InternalServerError("Sorry, something went wrong.")
    }.recover { case scala.util.control.NonFatal(e) =>
      lila.log("http").error(s"""Error handler exception on "${exception.getMessage}\"""", e)
      InternalServerError("Sorry, something went very wrong.")
    }

  override def onClientError(req: RequestHeader, statusCode: Int, msg: String): Fu[Result] =
    statusCode match
      case 400 | 404 if canShowErrorPage(req) => mainC.handlerNotFound(msg.nonEmptyOption)(using req)
      case 400 | 404 => fuccess(NotFound(s"$statusCode - ${msg.nonEmptyOption | "Resource not found"}"))
      case 403 => lobbyC.handleStatus(Results.Forbidden)(using req)
      case _ if canShowErrorPage(req) => lobbyC.handleStatus(Results.BadRequest)(using req)
      case _ => fuccess(Results.BadRequest("Sorry, the request could not be processed"))

  private def canShowErrorPage(req: RequestHeader): Boolean =
    HTTPRequest.isSynchronousHttp(req) &&
      !HTTPRequest.hasFileExtension(req) &&
      req.attrs.contains(request.RequestAttrKey.Session)
