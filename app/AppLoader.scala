import play.api._
import play.api.inject.ApplicationLifecycle

final class LilaLoader extends ApplicationLoader {

  // def load(context: Context) = {
  //   val application = new GuiceApplicationBuilder(
  //     environment = context.environment,
  //     configuration = context.initialConfiguration
  //   ).build()
  //   Env._ref.set(application)
  //   application
  // }

  def load(context: ApplicationLoader.Context): Application = {
    val components = new LilaComponents(context)
    val app = components.application
    old.play.Env.start(app)
    startKamon(context.lifecycle)
    lila.app.Env.current
    app
  }

  private def startKamon(lifecycle: ApplicationLifecycle) = {
    kamon.Kamon.start()
    lifecycle.addStopHook { () => lila.common.fuccess(kamon.Kamon.shutdown()) }
  }
}

final class LilaComponents(context: ApplicationLoader.Context)
  extends BuiltInComponentsFromContext(context) {

  def httpFilters = Nil
}
