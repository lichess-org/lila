package lila.streamer

import akka.actor.*
import com.softwaremill.macwire.*
import play.api.{ ConfigLoader, Configuration }

import lila.common.Bus
import lila.common.autoconfig.{ *, given }
import lila.common.config.{ *, given }
import lila.core.config.*

@Module
private class StreamerConfig(
    @ConfigName("collection.streamer") val streamerColl: CollName,
    @ConfigName("paginator.max_per_page") val paginatorMaxPerPage: MaxPerPage,
    @ConfigName("keyword") val keyword: Stream.Keyword,
    @ConfigName("youtube") val youtubeConfig: YoutubeConfig,
    @ConfigName("twitch") val twitchConfig: TwitchConfig
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
    net: lila.core.config.NetConfig,
    langList: lila.core.i18n.LangList
)(using scheduler: Scheduler)(using Executor, akka.stream.Materializer):

  private given ConfigLoader[TwitchConfig] = AutoConfig.loader[TwitchConfig]
  private given ConfigLoader[YoutubeConfig] = AutoConfig.loader[YoutubeConfig]
  private given ConfigLoader[Stream.Keyword] = strLoader(Stream.Keyword.apply)
  private val config = appConfig.get[StreamerConfig]("streamer")(using AutoConfig.loader)

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

  lazy val repo = wire[StreamerRepo]
  lazy val ytApi: YoutubeApi = wire[YoutubeApi]
  lazy val api: StreamerApi = wire[StreamerApi]

  lazy val pager = wire[StreamerPager]

  lazy val oauth = wire[StreamerOauth]

  lazy val twitchApi: TwitchApi = wire[TwitchApi]

  private val publisher = Publisher(
    api = api,
    repo = repo,
    isOnline = isOnline,
    keyword = config.keyword,
    alwaysFeatured = () => alwaysFeaturedSetting.get(),
    twitchApi = twitchApi,
    ytApi = ytApi,
    langList = langList
  )

  lazy val liveApi = wire[LiveApi]

  Bus.sub[lila.core.mod.MarkCheater]:
    case lila.core.mod.MarkCheater(userId, true) => api.demote(userId)
  Bus.sub[lila.core.mod.MarkBooster]: m =>
    api.demote(m.userId)
  Bus.sub[lila.core.mod.Shadowban]:
    case lila.core.mod.Shadowban(userId, true) => api.demote(userId)
    case lila.core.mod.Shadowban(userId, false) => repo.unignore(userId)

  lila.common.Cli.handle:
    case "streamer" :: "twitch" :: "resync" :: Nil =>
      twitchApi.syncAll.inject("done")
    case "streamer" :: "twitch" :: "resub" :: Nil =>
      twitchApi.subscribeAll.inject("done")
    case "streamer" :: "twitch" :: "show" :: Nil =>
      fuccess(twitchApi.debugLives)

  scheduler.scheduleWithFixedDelay(1.hour, 1.day): () =>
    repo.autoDemoteFakes

  scheduler.scheduleWithFixedDelay(21.minutes, 8.days): () =>
    ytApi.subscribeAll

  if config.twitchConfig.clientId.nonEmpty then
    scheduler.scheduleWithFixedDelay(30.seconds, 1.day): () =>
      twitchApi.syncAll
    scheduler.scheduleWithFixedDelay(72.seconds, 1.day): () =>
      twitchApi.subscribeAll
    scheduler.scheduleWithFixedDelay(4.minutes, 4.minutes): () =>
      twitchApi.checkThatLiveStreamersReallyAreLive
