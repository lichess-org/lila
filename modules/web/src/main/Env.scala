package lila.web

import com.softwaremill.macwire.*
import play.api.libs.ws.StandaloneWSClient

@Module
final class Env(
    appConfig: play.api.Configuration,
    cacheApi: lila.memo.CacheApi,
    settingStore: lila.memo.SettingStore.Builder,
    ws: StandaloneWSClient,
    net: lila.core.config.NetConfig,
    getFile: lila.common.config.GetRelativeFile
)(using mode: play.api.Mode, scheduler: Scheduler)(using Executor):

  val config = WebConfig.loadFrom(appConfig)
  export config.pagerDuty as pagerDutyConfig
  export net.baseUrl

  val analyseEndpoints = WebConfig.analyseEndpoints(appConfig)
  lazy val lilaVersion = WebConfig.lilaVersion(appConfig)

  lazy val mobile = wire[Mobile]

  val manifest = wire[AssetManifest]

  val referrerRedirect = wire[ReferrerRedirect]

  val github = wire[GitHub]

  private lazy val influxEvent = new InfluxEvent(
    ws = ws,
    endpoint = config.influxEventEndpoint,
    env = config.influxEventEnv
  )
  if mode.isProd then scheduler.scheduleOnce(5.seconds)(influxEvent.start())

  wire[PagerDuty]

  AnnounceApi.setupPeriodicUpdate()

  object settings:
    import lila.core.data.{ Strings, UserIds }
    import lila.memo.SettingStore.Strings.given
    import lila.memo.SettingStore.UserIds.given

    val apiTimeline = settingStore[Int](
      "apiTimelineEntries",
      default = 10,
      text = "API timeline entries to serve".some
    )
    val noDelaySecret = settingStore[Strings](
      "noDelaySecrets",
      default = Strings(Nil),
      text = "Secret tokens that allows fetching ongoing games without the delay. Separated by commas.".some
    )
    val prizeTournamentMakers = settingStore[UserIds](
      "prizeTournamentMakers",
      default = UserIds(Nil),
      text =
        "User IDs who can make prize tournaments (arena & swiss) without a warning. Separated by commas.".some
    )
    val apiExplorerGamesPerSecond = settingStore[Int](
      "apiExplorerGamesPerSecond",
      default = 300,
      text = "Opening explorer games per second".some
    )
