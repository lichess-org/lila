package lila.web

import play.api.libs.ws.StandaloneWSClient
import com.softwaremill.macwire.*
import lila.core.config.BaseUrl
import play.api.Configuration

@Module
final class Env(
    appConfig: Configuration,
    cacheApi: lila.memo.CacheApi,
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
