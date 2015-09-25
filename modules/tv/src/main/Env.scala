package lila.tv

import com.typesafe.config.Config
import akka.actor._

import lila.common.PimpedConfig._

import scala.collection.JavaConversions._

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    lightUser: String => Option[lila.common.LightUser],
    system: ActorSystem,
    scheduler: lila.common.Scheduler,
    isProd: Boolean) {

  private val FeaturedSelect = config duration "featured.select"
  private val StreamingSearch = config duration "streaming.search"
  private val UstreamApiKey = config getString "streaming.ustream_api_key"
  private val CollectionWhitelist = config getString "streaming.collection.whitelist"

  lazy val tv = new Tv(tvActor)

  private val tvActor =
    system.actorOf(
      Props(classOf[TvActor], hub.actor.renderer, hub.socket.round, lightUser),
      name = "tv")

  private lazy val streaming = new Streaming(
    system = system,
    renderer = hub.actor.renderer,
    ustreamApiKey = UstreamApiKey,
    whitelist = whitelist)

  private lazy val whitelist = new Whitelist(db(CollectionWhitelist))

  def streamsOnAir = streaming.onAir

  {
    import scala.concurrent.duration._

    scheduler.message(FeaturedSelect) {
      tvActor -> TvActor.Select
    }

    scheduler.once(20.seconds) {
      streaming.actor ! Streaming.Search
      scheduler.message(StreamingSearch) {
        streaming.actor -> Streaming.Search
      }
    }
  }
}

object Env {

  lazy val current = "tv" boot new Env(
    config = lila.common.PlayApp loadConfig "tv",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    lightUser = lila.user.Env.current.lightUser,
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler,
    isProd = lila.common.PlayApp.isProd)
}

