package lila.app

import play.api.http._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.routing.Router
import play.api.{ Environment, UsefulException }
import play.core.SourceMapper

import lila.common.HTTPRequest

class ErrorHandler(
    env: Environment,
    config: HttpErrorConfig,
    sourceMapper: Option[SourceMapper],
    router: Router
) extends DefaultHttpErrorHandler(config, sourceMapper, router.some) {

  private val httpLogger = lila.log("http")

  private def logHttp(code: Int, req: RequestHeader, exception: Option[Throwable] = None) = {
    val message = s"$code ${HTTPRequest print req}"
    exception match {
      case Some(e) => httpLogger.warn(message, e)
      case None => httpLogger.info(message)
    }
  }

  override def onProdServerError(req: RequestHeader, exception: UsefulException) = fuccess {
    logHttp(500, req, exception.some)
    if (niceError(req)) {
      lila.mon.http.response.code500()
      InternalServerError(views.html.base.errorPage(exception) {
        lila.api.Context(req, lila.app.Env.api.assetVersion.get, lila.i18n.defaultLang)
      })
    } else InternalServerError(exception.getMessage)
  }

  override def onClientError(req: RequestHeader, statusCode: Int, message: String) = {
    logHttp(statusCode, req)
    if (message startsWith "Illegal character in path") fuccess(Redirect("/"))
    else if (niceError(req)) {
      lila.mon.http.response.code400()
      statusCode match {
        case 404 => controllers.Main.notFound(req)
        case _ if (message startsWith "Cannot parse parameter") => controllers.Main.notFound(req)
        case _ => controllers.Lobby.handleStatus(req, Results.BadRequest)
      }
    } else super.onClientError(req, statusCode, message)
  }

  private def niceError(req: RequestHeader): Boolean =
    req.method == "GET" &&
      HTTPRequest.isSynchronousHttp(req) &&
      !HTTPRequest.hasFileExtension(req)

  // def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
  //   fuccess(
  //     Status(statusCode)("A client error occurred: " + message)
  //   )
  // }
}
