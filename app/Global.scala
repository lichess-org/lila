package lila.app

import play.api.{ Application, GlobalSettings, Mode }
import play.api.mvc._
import play.api.mvc.Results._

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    // if (env.ai.isServer) println("Running as AI server")
    // else if (env.settings.CoreCronEnabled) {
    //   println("Enable cron tasks")
    //   core.Cron start env
    // }
  }

  override def onRouteRequest(req: RequestHeader): Option[Handler] =
    // if (env.ai.isServer) {
    // if (req.path startsWith "/ai/") super.onRouteRequest(req)
    // else Action(NotFound("I am an AI server")).some
    // }
    // else {
    // req.queryString get "embed" flatMap (_.headOption) filter (""!=) foreach { embed ⇒
    //   println("[embed] %s -> %s".format(embed, req.path))
    // }
    // env.monitor.rpsProvider.countRequest()
    // env.security.wiretap(req)
    // env.security.firewall.requestHandler(req) orElse
    //   env.i18n.requestHandler(req) orElse
    //   super.onRouteRequest(req)
    // }
    super.onRouteRequest(req) ~ { _ ⇒ println(req) }

  // override def onHandlerNotFound(req: RequestHeader): Result =
  //   env.ai.isServer.fold(NotFound, controllers.Lobby handleNotFound req)

  override def onBadRequest(req: RequestHeader, error: String) = {
    BadRequest("Bad Request: " + error)
  }

  // override def onError(request: RequestHeader, ex: Throwable) =
  //   env.ai.isServer.fold(
  //     InternalServerError(ex.getMessage),
  //     Option(coreEnv).fold(Mode.Prod)(_.app.mode) match {
  //       case Mode.Prod ⇒ InternalServerError(
  //         views.html.base.errorPage(ex)(http.Context(request, none))
  //       )
  //       case _ ⇒ super.onError(request, ex)
  //     }
  //   )
}
