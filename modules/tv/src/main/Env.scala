package lila.tv

import com.typesafe.config.Config

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    hub: lila.hub.Env,
    system: akka.actor.ActorSystem,
    scheduler: lila.common.Scheduler) {

  private val FeaturedContinue = config duration "featured.continue"
  private val FeaturedDisrupt = config duration "featured.disrupt"

  lazy val featured = new Featured(
    lobbySocket = hub.socket.lobby,
    rendererActor = hub.actor.renderer,
    system = system)

  {
    import scala.concurrent.duration._

    scheduler.message(FeaturedContinue) {
      featured.actor -> Featured.Continue
    }

    scheduler.message(FeaturedDisrupt) {
      featured.actor -> Featured.Disrupt
    }
  }
}

object Env {

  lazy val current = "[boot] importer" describes new Env(
    config = lila.common.PlayApp loadConfig "tv",
    hub = lila.hub.Env.current,
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler)
}

