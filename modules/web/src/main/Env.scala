package lila.web

import play.api.libs.ws.StandaloneWSClient
import com.softwaremill.macwire.*
import lila.core.config.BaseUrl
import play.api.Configuration

@Module
final class Env(
    appConfig: Configuration,
    cacheApi: lila.memo.CacheApi,
    settingStore: lila.memo.SettingStore.Builder,
    ws: StandaloneWSClient,
    baseUrl: BaseUrl
)(using mode: play.api.Mode, scheduler: Scheduler)(using Executor):

  val config = WebConfig.loadFrom(appConfig)
  export config.{ pagerDuty as pagerDutyConfig }

  val realPlayers = wire[RealPlayerApi]

  val referrerRedirect = wire[ReferrerRedirect]

  private lazy val influxEvent = new InfluxEvent(
    ws = ws,
    endpoint = config.influxEventEndpoint,
    env = config.influxEventEnv
  )
  if mode.isProd then scheduler.scheduleOnce(5 seconds)(influxEvent.start())
  private lazy val pagerDuty = wire[PagerDuty]

  lila.common.Bus.subscribeFun("announce"):
    case lila.core.socket.Announce(msg, date, _) if msg.contains("will restart") =>
      pagerDuty.lilaRestart(date)

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
      text =
        "Secret tokens that allows fetching ongoing games without the 3-moves delay. Separated by commas.".some
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
    val pieceImageExternal = settingStore[Boolean](
      "pieceImageExternal",
      default = false,
      text = "Use external piece images".some
    )
