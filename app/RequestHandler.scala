package lila.app

import play.api.http._
import play.api.mvc.RequestHeader
import play.api.routing.Router
import play.mvc.EssentialFilter

class RequestHandler(
    router: Router,
    errorHandler: HttpErrorHandler,
    configuration: HttpConfiguration,
    filters: Seq[EssentialFilter]
) extends DefaultHttpRequestHandler(router, errorHandler, configuration, filters: _*) {

  override def routeRequest(req: RequestHeader) = {
    lila.mon.http.request.all()
    if (req.remoteAddress contains ":") lila.mon.http.request.ipv6()
    lila.i18n.Env.current.subdomainKiller(req) orElse super.routeRequest(req)
  }
}
