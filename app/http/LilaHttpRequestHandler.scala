package lila.app
package http

import play.api.http.{ DefaultHttpRequestHandler, HttpConfiguration, HttpErrorHandler, HttpRequestHandler }
import play.api.mvc.{ EssentialFilter, Handler, RequestHeader }
import play.api.routing.Router

import lila.common.Chronometer

final class LilaHttpRequestHandler(
    router: Router,
    errorHandler: HttpErrorHandler,
    configuration: HttpConfiguration,
    filters: Seq[EssentialFilter]
) extends DefaultHttpRequestHandler(() => router, errorHandler, configuration, filters) {

  private val monitorPaths = Set("/tv", "/robots.txt")

  override def routeRequest(request: RequestHeader): Option[Handler] =
    if (monitorPaths(request.path))
      Chronometer.syncMon(_.http.router(request.path)) {
        router handlerFor request
      }
    else router handlerFor request
}
