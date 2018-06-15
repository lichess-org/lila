package lila.streamer

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    system: ActorSystem,
    renderer: ActorSelection,
    isOnline: lila.user.User.ID => Boolean,
    asyncCache: lila.memo.AsyncCache.Builder,
    notifyApi: lila.notify.NotifyApi,
    lightUserApi: lila.user.LightUserApi,
    hub: lila.hub.Env,
    db: lila.db.Env
) {

  private val CollectionStreamer = config getString "collection.streamer"
  private val CollectionImage = config getString "collection.image"
  private val MaxPerPage = config getInt "paginator.max_per_page"
  private val Keyword = config getString "streaming.keyword"
  private val GoogleApiKey = config getString "streaming.google.api_key"
  private val TwitchClientId = config getString "streaming.twitch.client_id"

  private lazy val streamerColl = db(CollectionStreamer)
  private lazy val imageColl = db(CollectionImage)

  private lazy val photographer = new lila.db.Photographer(imageColl, "streamer")

  lazy val api = new StreamerApi(
    coll = streamerColl,
    asyncCache = asyncCache,
    photographer = photographer,
    notifyApi = notifyApi
  )

  lazy val pager = new StreamerPager(
    coll = streamerColl,
    maxPerPage = lila.common.MaxPerPage(MaxPerPage)
  )

  private val streamingActor = system.actorOf(Props(new Streaming(
    renderer = renderer,
    api = api,
    isOnline = isOnline,
    timeline = hub.actor.timeline,
    keyword = Stream.Keyword(Keyword),
    googleApiKey = GoogleApiKey,
    twitchClientId = TwitchClientId,
    lightUserApi = lightUserApi
  )))

  lazy val liveStreamApi = new LiveStreamApi(asyncCache, streamingActor)

  system.lilaBus.subscribe(
    system.actorOf(Props(new Actor {
      def receive = {
        case lila.user.User.Active(user) if !user.seenRecently => api setSeenAt user
        case lila.hub.actorApi.mod.MarkCheater(userId, true) => api demote userId
      }
    })), 'userActive, 'adjustCheater
  )
}

object Env {

  lazy val current: Env = "streamer" boot new Env(
    config = lila.common.PlayApp loadConfig "streamer",
    system = lila.common.PlayApp.system,
    renderer = lila.hub.Env.current.actor.renderer,
    isOnline = lila.user.Env.current.isOnline,
    asyncCache = lila.memo.Env.current.asyncCache,
    notifyApi = lila.notify.Env.current.api,
    lightUserApi = lila.user.Env.current.lightUserApi,
    hub = lila.hub.Env.current,
    db = lila.db.Env.current
  )
}
