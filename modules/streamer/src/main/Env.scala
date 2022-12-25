package lila.streamer

import akka.actor.*
import com.softwaremill.macwire.*
import lila.common.autoconfig.{ *, given }
import play.api.{ ConfigLoader, Configuration }
import scala.concurrent.duration.*

import lila.common.config.*

@Module
private class StreamerConfig(
    @ConfigName("collection.streamer") val streamerColl: CollName,
    @ConfigName("paginator.max_per_page") val paginatorMaxPerPage: MaxPerPage,
    @ConfigName("streaming.keyword") val keyword: Stream.Keyword,
    @ConfigName("streaming.google.api_key") val googleApiKey: Secret,
    @ConfigName("streaming.twitch") val twitchConfig: TwitchConfig
)
private class TwitchConfig(@ConfigName("client_id") val clientId: String, val secret: Secret)

@Module
final class Env(
    appConfig: Configuration,
    ws: play.api.libs.ws.StandaloneWSClient,
    settingStore: lila.memo.SettingStore.Builder,
    isOnline: lila.socket.IsOnline,
    cacheApi: lila.memo.CacheApi,
    picfitApi: lila.memo.PicfitApi,
    notifyApi: lila.notify.NotifyApi,
    userRepo: lila.user.UserRepo,
    subsRepo: lila.relation.SubscriptionRepo,
    prefApi: lila.pref.PrefApi,
    db: lila.db.Db
)(using
    ec: scala.concurrent.ExecutionContext,
    scheduler: akka.actor.Scheduler
):

  private given ConfigLoader[TwitchConfig]   = AutoConfig.loader[TwitchConfig]
  private given ConfigLoader[Stream.Keyword] = strLoader(Stream.Keyword.apply)
  private val config                         = appConfig.get[StreamerConfig]("streamer")(AutoConfig.loader)

  private lazy val streamerColl = db(config.streamerColl)

  lazy val alwaysFeaturedSetting =
    import lila.memo.SettingStore.UserIds.given
    import lila.common.UserIds
    settingStore[UserIds](
      "streamerAlwaysFeatured",
      default = UserIds(Nil),
      text = "Twitch streamers featured without the keyword - lichess usernames separated by a comma".some
    )

  lazy val homepageMaxSetting =
    settingStore[Int](
      "streamerHomepageMax",
      default = 6,
      text = "Max streamers on homepage".some
    )

  lazy val api: StreamerApi = wire[StreamerApi]

  lazy val pager = wire[StreamerPager]

  private lazy val twitchApi: TwitchApi = wire[TwitchApi]

  private val streaming = new Streaming(
    ws = ws,
    api = api,
    isOnline = isOnline,
    keyword = config.keyword,
    alwaysFeatured = (() => alwaysFeaturedSetting.get()),
    googleApiKey = config.googleApiKey,
    twitchApi = twitchApi
  )

  lazy val liveStreamApi = wire[LiveStreamApi]

  lila.common.Bus.subscribeFun("adjustCheater") { case lila.hub.actorApi.mod.MarkCheater(userId, true) =>
    api.demote(userId).unit
  }

  scheduler.scheduleWithFixedDelay(1 hour, 1 day) { () =>
    api.autoDemoteFakes.unit
  }
