package lila.app

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import javax.inject._
import lila.common.HTTPRequest
import play.api._
import play.api.http._
import play.api.inject.ApplicationLifecycle
import play.api.mvc._
import play.api.mvc.RequestHeader
import play.api.mvc.Results._
import play.api.routing.Router
import scala.concurrent._

class LilaHttpRequestHandler @Inject() (errorHandler: HttpErrorHandler,
    configuration: HttpConfiguration, filters: HttpFilters,
    router: Router) extends DefaultHttpRequestHandler(router, errorHandler, configuration, filters) {

  override def routeRequest(req: RequestHeader) = {
    lila.mon.http.request.all()
    Env.i18n.requestHandler(req) orElse super.routeRequest(req)
  }
}

class LilaHttpErrorHandler @Inject() (
    env: Environment,
    config: Configuration,
    sourceMapper: OptionalSourceMapper,
    router: Provider[Router]) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {

  private def niceError(req: RequestHeader): Boolean =
    req.method == "GET" &&
      HTTPRequest.isSynchronousHttp(req) &&
      !HTTPRequest.hasFileExtension(req)

  def onHandlerNotFound(req: RequestHeader) =
    if (niceError(req)) controllers.Main.notFound(req)
    else fuccess(NotFound("404 - Resource not found"))

  override def onBadRequest(req: RequestHeader, error: String) =
    if (error startsWith "Illegal character in path") fuccess(Redirect("/"))
    else if (error startsWith "Cannot parse parameter") onHandlerNotFound(req)
    else if (niceError(req)) {
      lila.mon.http.response.code400()
      controllers.Lobby.handleStatus(req, Results.BadRequest)
    }
    else fuccess(BadRequest(error))

  override def onProdServerError(req: RequestHeader, ex: UsefulException): Future[Result] = {
    lila.mon.http.response.code500()
    fuccess(InternalServerError(views.html.base.errorPage(ex)(lila.api.Context(req))))
  }
}

@Singleton
final class LilaLifecycle @Inject() (lifecycle: ApplicationLifecycle) {

  play.api.Logger("boot").info("Lifecycle bindings")

  lifecycle.addStopHook { () =>
    play.api.Logger("play").info("LilaLifecycle shutdown")
    kamon.Kamon.shutdown()
    funit
  }
}

final class LilaModule extends AbstractModule {

  play.api.Logger("boot").info("Kamon start")
  kamon.Kamon.start()

  def configure() = {
    bind(classOf[LilaLifecycle]).asEagerSingleton
  }
}
