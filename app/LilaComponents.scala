package lila.app

import play.api.routing.Router
import play.api.{ ApplicationLoader, BuiltInComponentsFromContext }
import play.mvc.EssentialFilter

final class LilaComponents(context: ApplicationLoader.Context, val router: Router)
  extends BuiltInComponentsFromContext(context) {

  override def httpFilters = List.empty[EssentialFilter]

  override lazy val httpErrorHandler = new ErrorHandler(
    environment,
    play.api.http.HttpErrorConfig(
      showDevErrors = environment.mode != play.api.Mode.Prod,
      playEditor = None
    ),
    sourceMapper,
    router
  )

  override lazy val httpRequestHandler = new RequestHandler(
    router,
    httpErrorHandler,
    httpConfiguration,
    httpFilters
  )
}
