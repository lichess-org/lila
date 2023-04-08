package lila.app
package http

import play.api.http.{ DefaultHttpRequestHandler, HttpConfiguration, HttpErrorHandler }
import play.api.mvc.{ ControllerComponents, EssentialFilter, Handler, RequestHeader, Results }
import play.api.routing.Router

final class LilaHttpRequestHandler(
    router: Router,
    errorHandler: HttpErrorHandler,
    configuration: HttpConfiguration,
    filters: Seq[EssentialFilter],
    controllerComponents: ControllerComponents
) extends DefaultHttpRequestHandler(() => router, errorHandler, configuration, filters):

  override def routeRequest(request: RequestHeader): Option[Handler] =
    if (request.method == "OPTIONS") optionsHandler.some
    else router handlerFor request

  // should be handled by nginx in production
  private val optionsHandler =
    controllerComponents.actionBuilder { (req: RequestHeader) =>
      if (lila.common.HTTPRequest.isApiOrApp(req))
        Results.NoContent.withHeaders(
          "Allow"                  -> ResponseHeaders.allowMethods,
          "Access-Control-Max-Age" -> "86400"
        )
      else Results.NotFound
    }
