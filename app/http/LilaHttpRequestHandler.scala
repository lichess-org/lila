package lila.app
package http

import play.api.ApplicationLoader.DevContext
import play.api.http.DefaultHttpRequestHandler
import play.api.http.HttpConfiguration
import play.api.http.HttpErrorHandler
import play.api.mvc.ControllerComponents
import play.api.mvc.EssentialFilter
import play.api.mvc.Handler
import play.api.mvc.RequestHeader
import play.api.mvc.Results
import play.api.routing.Router
import play.core.WebCommands

final class LilaHttpRequestHandler(
    webCommands: WebCommands,
    devContext: Option[DevContext],
    router: Router,
    errorHandler: HttpErrorHandler,
    configuration: HttpConfiguration,
    filters: Seq[EssentialFilter],
    controllerComponents: ControllerComponents,
) extends DefaultHttpRequestHandler(
      webCommands,
      devContext,
      () => router,
      errorHandler,
      configuration,
      filters,
    ) {

  override def routeRequest(request: RequestHeader): Option[Handler] =
    if (request.method == "OPTIONS") optionsHandler.some
    else router handlerFor request

  private val optionsHandler =
    controllerComponents.actionBuilder { req =>
      if (lila.common.HTTPRequest.isApiOrApp(req))
        Results.NoContent.withHeaders(
          "Allow"                  -> ResponseHeaders.allowMethods,
          "Access-Control-Max-Age" -> "86400",
        )
      else Results.NotFound
    }
}
