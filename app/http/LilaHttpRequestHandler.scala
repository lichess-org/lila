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

  override def handlerForRequest(request: RequestHeader): (RequestHeader, Handler) =
    Chronometer.syncMon(_.http.requestHandler) {
      super.handlerForRequest(request)
    }

  override def routeRequest(request: RequestHeader): Option[Handler] =
    Chronometer.syncMon(_.http.router) {
      router handlerFor request
    }
}
