package lila.app

import play.api._
import play.api.inject.ApplicationLifecycle

final class LilaLoader extends ApplicationLoader {

  def load(context: ApplicationLoader.Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }
    val components = new LilaComponents(context, _root_.router.Routes)
    val app = components.application
    old.play.Env.start(old.play.Env.Deps(
      components.application,
      components.applicationLifecycle,
      components.executionContext,
      components.environment,
      components.controllerComponents
    ))
    startKamon(context.lifecycle)
    lila.app.Env.current
    app
  }

  private def startKamon(lifecycle: ApplicationLifecycle) = {
    kamon.Kamon.start()
    lifecycle.addStopHook { () => lila.common.fuccess(kamon.Kamon.shutdown()) }
  }
}
