package lila.tv

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._
import lila.db.dsl._
import lila.memo.AsyncCache

import scala.concurrent.duration._

final class Env(
    config: Config,
    db: lila.db.Env,
    hub: lila.hub.Env,
    lightUser: lila.common.LightUser.GetterSync,
    system: ActorSystem,
    scheduler: lila.common.Scheduler,
    isProd: Boolean) {

  private val FeaturedSelect = config duration "featured.select"
  private val StreamingSearch = config duration "streaming.search"
  private val GoogleApiKey = config getString "streaming.google.api_key"
  private val Keyword = config getString "streaming.keyword"
  private val HitboxUrl = config getString "streaming.hitbox.url"
  private val TwitchClientId = config getString "streaming.twitch.client_id"
  private val ChannelSelect = config getString "channel.select.name "

  private val selectChannel = system.actorOf(Props(classOf[lila.socket.Channel]), name = ChannelSelect)

  lazy val tv = new Tv(tvActor)

  private val tvActor =
    system.actorOf(
      Props(new TvActor(hub.actor.renderer, hub.socket.round, selectChannel, lightUser)),
      name = "tv")

  private lazy val streaming = new Streaming(
    system = system,
    renderer = hub.actor.renderer,
    streamerList = streamerList,
    keyword = Keyword,
    googleApiKey = GoogleApiKey,
    hitboxUrl = HitboxUrl,
    twitchClientId = TwitchClientId)

  lazy val streamerList = new StreamerList(new {
    import reactivemongo.bson._
    private val coll = db("flag")
    def get = coll.primitiveOne[String]($id("streamer"), "text") map (~_)
    def set(text: String) =
      coll.update($id("streamer"), $doc("text" -> text), upsert = true).void
  })

  object isStreamer {
    private val cache = AsyncCache.single[Set[lila.user.User.ID]](
      name = "tv.streamers",
      f = streamerList.lichessIds,
      timeToLive = 10 seconds)
    def apply(id: lila.user.User.ID): Fu[Boolean] = cache(true) map { _ contains id }
  }

  object streamsOnAir {
    private val cache = lila.memo.AsyncCache.single[List[StreamOnAir]](
      name = "tv.streamsOnAir",
      f = streaming.onAir,
      timeToLive = 2 seconds)
    def all = cache(true)
  }

  {
    import scala.concurrent.duration._

    scheduler.message(FeaturedSelect) {
      tvActor -> TvActor.Select
    }

    scheduler.once(2.seconds) {
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
    lightUser = lila.user.Env.current.lightUserSync,
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler,
    isProd = lila.common.PlayApp.isProd)
}

