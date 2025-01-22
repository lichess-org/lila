package lila.streamer

import akka.actor.*
import com.softwaremill.macwire.*
import play.api.{ ConfigLoader, Configuration }

import lila.common.autoconfig.{ *, given }
import lila.common.config.{ *, given }
import lila.core.config.*

@Module
private class StreamerConfig(
    @ConfigName("collection.streamer") val streamerColl: CollName,
    @ConfigName("paginator.max_per_page") val paginatorMaxPerPage: MaxPerPage,
    @ConfigName("streaming.keyword") val keyword: Stream.Keyword,
    @ConfigName("streaming.google.api_key") val googleApiKey: Secret,
    @ConfigName("streaming.twitch") val twitchConfig: TwitchConfig
)
private class TwitchConfig(
    val endpoint: String,
    @ConfigName("client_id") val clientId: String,
    val secret: Secret
)

@Module
final class Env(
    appConfig: Configuration,
    ws: play.api.libs.ws.StandaloneWSClient,
    settingStore: lila.memo.SettingStore.Builder,
    isOnline: lila.core.socket.IsOnline,
    cacheApi: lila.memo.CacheApi,
    picfitApi: lila.memo.PicfitApi,
    notifyApi: lila.core.notify.NotifyApi,
    userRepo: lila.core.user.UserRepo,
    userApi: lila.core.user.UserApi,
    subsRepo: lila.core.relation.SubscriptionRepo,
    db: lila.db.Db,
    net: lila.core.config.NetConfig
)(using scheduler: Scheduler)(using Executor, akka.stream.Materializer):

  private given ConfigLoader[TwitchConfig]   = AutoConfig.loader[TwitchConfig]
  private given ConfigLoader[Stream.Keyword] = strLoader(Stream.Keyword.apply)
  private val config                         = appConfig.get[StreamerConfig]("streamer")(AutoConfig.loader)

  private lazy val streamerColl = db(config.streamerColl)

  lazy val alwaysFeaturedSetting =
    import lila.memo.SettingStore.UserIds.given
    import lila.core.data.UserIds
    settingStore[UserIds](
      "streamerAlwaysFeatured",
      default = UserIds(Nil),
      text = "Twitch streamers featured without the keyword - lichess usernames separated by a comma".some
    )

  lazy val homepageMaxSetting =
    settingStore[Int](
      "streamerHomepageMax",
      default = 5,
      text = "Max streamers on homepage".some
    )

  lazy val ytApi: YouTubeApi = wire[YouTubeApi]
  lazy val api: StreamerApi  = wire[StreamerApi]

  lazy val pager = wire[StreamerPager]

  private lazy val twitchApi: TwitchApi = wire[TwitchApi]

  private val streaming = Streaming(
    api = api,
    isOnline = isOnline,
    keyword = config.keyword,
    alwaysFeatured = (() => alwaysFeaturedSetting.get()),
    twitchApi = twitchApi,
    ytApi = ytApi
  )

  lazy val liveStreamApi = wire[LiveStreamApi]

  lila.common.Bus.subscribeFun("adjustCheater", "adjustBooster", "shadowban"):
    case lila.core.mod.MarkCheater(userId, true) => api.demote(userId)
    case lila.core.mod.MarkBooster(userId)       => api.demote(userId)
    case lila.core.mod.Shadowban(userId, true)   => api.demote(userId)
    case lila.core.mod.Shadowban(userId, false)  => api.unignore(userId)

  scheduler.scheduleWithFixedDelay(1.hour, 1.day): () =>
    api.autoDemoteFakes
  scheduler.scheduleWithFixedDelay(21.minutes, 8.days): () =>
    ytApi.subscribeAll
