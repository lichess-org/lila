package lidraughts.streamer

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    system: ActorSystem,
    renderer: ActorSelection,
    isOnline: lidraughts.user.User.ID => Boolean,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    notifyApi: lidraughts.notify.NotifyApi,
    lightUserApi: lidraughts.user.LightUserApi,
    hub: lidraughts.hub.Env,
    db: lidraughts.db.Env
) {

  private val CollectionStreamer = config getString "collection.streamer"
  private val CollectionImage = config getString "collection.image"
  private val MaxPerPage = config getInt "paginator.max_per_page"
  private val Keyword = config getString "streaming.keyword"
  private val GoogleApiKey = config getString "streaming.google.api_key"
  private val TwitchClientId = config getString "streaming.twitch.client_id"

  private lazy val streamerColl = db(CollectionStreamer)
  private lazy val imageColl = db(CollectionImage)

  private lazy val photographer = new lidraughts.db.Photographer(imageColl, "streamer")

  lazy val api = new StreamerApi(
    coll = streamerColl,
    asyncCache = asyncCache,
    photographer = photographer,
    notifyApi = notifyApi
  )

  lazy val pager = new StreamerPager(
    coll = streamerColl,
    maxPerPage = lidraughts.common.MaxPerPage(MaxPerPage)
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

  system.lidraughtsBus.subscribe(
    system.actorOf(Props(new Actor {
      def receive = {
        case lidraughts.user.User.Active(user) if !user.seenRecently => api.setSeenAt(user)
      }
    })), 'userActive
  )
}

object Env {

  lazy val current: Env = "streamer" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "streamer",
    system = lidraughts.common.PlayApp.system,
    renderer = lidraughts.hub.Env.current.actor.renderer,
    isOnline = lidraughts.user.Env.current.isOnline,
    asyncCache = lidraughts.memo.Env.current.asyncCache,
    notifyApi = lidraughts.notify.Env.current.api,
    lightUserApi = lidraughts.user.Env.current.lightUserApi,
    hub = lidraughts.hub.Env.current,
    db = lidraughts.db.Env.current
  )
}
