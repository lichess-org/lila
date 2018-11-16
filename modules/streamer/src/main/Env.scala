package lidraughts.streamer

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    system: ActorSystem,
    settingStore: lidraughts.memo.SettingStore.Builder,
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

  lazy val alwaysFeaturedSetting = {
    val stringListIso = lidraughts.common.Iso.stringList(",")
    implicit val stringListBsonHandler = lidraughts.db.dsl.isoHandler(stringListIso)
    implicit val stringListReader = lidraughts.memo.SettingStore.StringReader.fromIso(stringListIso)
    settingStore[List[lidraughts.user.User.ID]](
      "streamerAlwaysFeatured",
      default = Nil,
      text = "Twitch streamers who get featured without the keyword".some
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
    maxPerPage = lidraughts.common.MaxPerPage(MaxPerPage)
  )

  private val streamingActor = system.actorOf(Props(new Streaming(
    renderer = renderer,
    api = api,
    isOnline = isOnline,
    timeline = hub.actor.timeline,
    keyword = Stream.Keyword(Keyword),
    alwaysFeatured = alwaysFeaturedSetting.get,
    googleApiKey = GoogleApiKey,
    twitchClientId = TwitchClientId,
    lightUserApi = lightUserApi
  )))

  lazy val liveStreamApi = new LiveStreamApi(asyncCache, streamingActor)

  system.lidraughtsBus.subscribeFun('userActive, 'adjustCheater) {
    case lidraughts.user.User.Active(user) if !user.seenRecently => api setSeenAt user
    case lidraughts.hub.actorApi.mod.MarkCheater(userId, true) => api demote userId
  }
}

object Env {

  lazy val current: Env = "streamer" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "streamer",
    system = lidraughts.common.PlayApp.system,
    settingStore = lidraughts.memo.Env.current.settingStore,
    renderer = lidraughts.hub.Env.current.actor.renderer,
    isOnline = lidraughts.user.Env.current.isOnline,
    asyncCache = lidraughts.memo.Env.current.asyncCache,
    notifyApi = lidraughts.notify.Env.current.api,
    lightUserApi = lidraughts.user.Env.current.lightUserApi,
    hub = lidraughts.hub.Env.current,
    db = lidraughts.db.Env.current
  )
}
