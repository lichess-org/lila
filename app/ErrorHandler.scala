package lila.app

import play.api.http._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.routing.Router
import play.api.{ Environment, UsefulException }
import play.core.SourceMapper

import lila.common.{ HTTPRequest, AssetVersion }

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

  override def onProdServerError(req: RequestHeader, ex: UsefulException) = {
    logHttp(500, req, ex.some)
    if (niceError(req)) {
      lila.mon.http.response.code500()
      fuccess(InternalServerError(views.html.base.errorPage(ex) {
        lila.api.Context.error(
          req,
          lila.common.AssetVersion(lila.app.Env.api.assetVersionSetting.get()),
          lila.i18n.defaultLang,
          HTTPRequest.isSynchronousHttp(req) option lila.common.Nonce.random
        )
      }))
    } else scala.concurrent.Future {
      InternalServerError(ex.getMessage)
    } recover {
      // java.lang.NullPointerException: null
      // at play.api.mvc.Codec$$anonfun$javaSupported$1.apply(Results.scala:320) ~[com.typesafe.play.play_2.11-2.4.11.jar:2.4.11]
      case e: java.lang.NullPointerException =>
        httpLogger.warn(s"""error handler exception on "${ex.getMessage}\"""", e)
        InternalServerError("Something went wrong.")
    }
  }

  override def onClientError(req: RequestHeader, statusCode: Int, message: String) = {
    lila.mon.http.response.code400()
    if (message startsWith "Illegal character in path") fuccess(Redirect("/"))
    else statusCode match {
      case 404 => onHandlerNotFound(req)
      case _ if message startsWith "Cannot parse parameter" => controllers.Main.notFound(req)
      case _ if niceError(req) =>
        logHttp(statusCode, req)
        controllers.Lobby.handleStatus(req, Results.BadRequest)
      case _ => fuccess(BadRequest(message))
    }
  }

  private def onHandlerNotFound(req: RequestHeader) =
    if (niceError(req)) {
      logHttp(404, req)
      controllers.Main.notFound(req)
    } else fuccess(NotFound("404 - Resource not found"))

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
