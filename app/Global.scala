package lila.app

import lila.common.HTTPRequest
import play.api.mvc._
import play.api.mvc.Results._
import play.api.{ Application, GlobalSettings }

object Global extends GlobalSettings {

  private val httpLogger = lila.log("http")

  private def logHttp(code: Int, req: RequestHeader, exception: Option[Throwable] = None) = {
    val message = s"$code ${HTTPRequest print req}"
    exception match {
      case Some(e) => httpLogger.warn(message, e)
      case None => httpLogger.info(message)
    }
  }

  override def onStart(app: Application): Unit = {
    kamon.Kamon.start()
    lila.app.Env.current
  }

  override def onStop(app: Application): Unit = {
    kamon.Kamon.shutdown()
  }

  override def onRouteRequest(req: RequestHeader): Option[Handler] = {
    lila.mon.http.request.all()
    if (req.remoteAddress contains ":") lila.mon.http.request.ipv6()
    lila.i18n.Env.current.subdomainKiller(req) orElse
      super.onRouteRequest(req)
  }

  private def niceError(req: RequestHeader): Boolean =
    req.method == "GET" &&
      HTTPRequest.isSynchronousHttp(req) &&
      !HTTPRequest.hasFileExtension(req)

  override def onHandlerNotFound(req: RequestHeader) =
    if (niceError(req)) {
      logHttp(404, req)
      controllers.Main.renderNotFound(req)
    } else fuccess(NotFound("404 - Resource not found"))

  override def onBadRequest(req: RequestHeader, error: String) = {
    logHttp(400, req)
    if (error startsWith "Illegal character in path") fuccess(Redirect("/"))
    else if (error startsWith "Cannot parse parameter") onHandlerNotFound(req)
    else if (niceError(req)) {
      lila.mon.http.response.code400()
      controllers.Lobby.handleStatus(req, Results.BadRequest)
    } else fuccess(BadRequest(error))
  }

  override def onError(req: RequestHeader, ex: Throwable) = {
    logHttp(500, req, ex.some)
    if (niceError(req)) {
      if (lila.common.PlayApp.isProd) {
        lila.mon.http.response.code500()
        fuccess(InternalServerError(views.html.base.errorPage(ex) {
          lila.api.Context.error(req, lila.common.AssetVersion(lila.app.Env.api.assetVersionSetting.get()), lila.i18n.defaultLang)
        }))
      } else super.onError(req, ex)
    } else fuccess(InternalServerError(ex.getMessage))
  }
}
