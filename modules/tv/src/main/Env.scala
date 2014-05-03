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
  private val StreamingSearch = config duration "streaming.search"

  lazy val featured = new Featured(
    lobbySocket = hub.socket.lobby,
    rendererActor = hub.actor.renderer,
    system = system)

  private lazy val streaming = new Streaming(
    system = system)

  def streamsOnAir = streaming.onAir

  {
    import scala.concurrent.duration._

    scheduler.message(FeaturedContinue) {
      featured.actor -> Featured.Continue
    }

    scheduler.message(FeaturedDisrupt) {
      featured.actor -> Featured.Disrupt
    }

    scheduler.message(StreamingSearch) {
      streaming.actor -> Streaming.Search
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

