package lila.streamer

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import scala.concurrent.duration._

import lila.common.config._

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
    timeline: lila.hub.actors.Timeline,
    db: lila.db.Db,
    imageRepo: lila.db.ImageRepo
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  implicit private val twitchLoader  = AutoConfig.loader[TwitchConfig]
  implicit private val keywordLoader = strLoader(Stream.Keyword.apply)
  private val config                 = appConfig.get[StreamerConfig]("streamer")(AutoConfig.loader)

  private lazy val streamerColl = db(config.streamerColl)

  private lazy val photographer = new lila.db.Photographer(imageRepo, "streamer")

  lazy val alwaysFeaturedSetting = {
    import lila.memo.SettingStore.UserIds._
    import lila.common.UserIds
    settingStore[UserIds](
      "streamerAlwaysFeatured",
      default = UserIds(Nil),
      text =
        "Twitch streamers who get featured without the keyword - lichess usernames separated by a comma".some
    )
  }

  lazy val homepageMaxSetting =
    settingStore[Int](
      "streamerHomepageMax",
      default = 6,
      text = "Max streamers on homepage".some
    )

  lazy val api: StreamerApi = wire[StreamerApi]

  lazy val pager = wire[StreamerPager]

  private lazy val twitchApi: TwitchApi = wire[TwitchApi]

  private val streamingActor = system.actorOf(
    Props(
      new Streaming(
        ws = ws,
        api = api,
        isOnline = isOnline,
        timeline = timeline,
        keyword = config.keyword,
        alwaysFeatured = alwaysFeaturedSetting.get _,
        googleApiKey = config.googleApiKey,
        twitchApi = twitchApi
      )
    )
  )

  lazy val liveStreamApi = wire[LiveStreamApi]

  lila.common.Bus.subscribeFun("adjustCheater") { case lila.hub.actorApi.mod.MarkCheater(userId, true) =>
    api.demote(userId).unit
  }

  system.scheduler.scheduleWithFixedDelay(1 hour, 1 day) { () =>
    api.autoDemoteFakes.unit
  }
}
