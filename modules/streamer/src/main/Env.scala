package lila.streamer

import akka.actor._
import com.typesafe.config.Config

import lila.common.Strings

final class Env(
    config: Config,
    system: ActorSystem,
    settingStore: lila.memo.SettingStore.Builder,
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

  lazy val alwaysFeaturedSetting = {
    import lila.memo.SettingStore.Strings._
    settingStore[Strings](
      "streamerAlwaysFeatured",
      default = Strings(Nil),
      text = "Twitch streamers who get featured without the keyword - lichess usernames separated by a comma".some
    )
  }

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
    timeline = hub.timeline,
    keyword = Stream.Keyword(Keyword),
    alwaysFeatured = alwaysFeaturedSetting.get,
    googleApiKey = GoogleApiKey,
    twitchClientId = TwitchClientId,
    lightUserApi = lightUserApi
  )))

  lazy val liveStreamApi = new LiveStreamApi(asyncCache, streamingActor)

  system.lilaBus.subscribeFun('userActive, 'adjustCheater) {
    case lila.user.User.Active(user) if !user.seenRecently => api setSeenAt user
    case lila.hub.actorApi.mod.MarkCheater(userId, true) => api demote userId
  }
}

object Env {

  lazy val current: Env = "streamer" boot new Env(
    config = lila.common.PlayApp loadConfig "streamer",
    system = lila.common.PlayApp.system,
    settingStore = lila.memo.Env.current.settingStore,
    renderer = lila.hub.Env.current.renderer,
    isOnline = lila.user.Env.current.isOnline,
    asyncCache = lila.memo.Env.current.asyncCache,
    notifyApi = lila.notify.Env.current.api,
    lightUserApi = lila.user.Env.current.lightUserApi,
    hub = lila.hub.Env.current,
    db = lila.db.Env.current
  )
}
