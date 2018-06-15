package lila.irwin

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.tournament.TournamentApi

final class Env(
    config: Config,
    system: ActorSystem,
    scheduler: lila.common.Scheduler,
    tournamentApi: TournamentApi,
    modApi: lila.mod.ModApi,
    reportApi: lila.report.ReportApi,
    notifyApi: lila.notify.NotifyApi,
    userCache: lila.user.Cached,
    settingStore: lila.memo.SettingStore.Builder,
    db: lila.db.Env
) {

  private val reportColl = db(config getString "collection.report")

  lazy val irwinModeSetting = settingStore[String](
    "irwinMode",
    default = "none",
    text = "Allow Irwin to: [mark|report|none]".some
  )

  val stream = new IrwinStream(system)

  lazy val api = new IrwinApi(
    reportColl = reportColl,
    modApi = modApi,
    reportApi = reportApi,
    notifyApi = notifyApi,
    bus = system.lilaBus,
    mode = irwinModeSetting.get
  )

  scheduler.future(5 minutes, "irwin tournament leaders") {
    tournamentApi.allCurrentLeadersInStandard flatMap api.requests.fromTournamentLeaders
  }
  scheduler.future(15 minutes, "irwin leaderboards") {
    userCache.getTop50Online flatMap api.requests.fromLeaderboard
  }
}

object Env {

  lazy val current: Env = "irwin" boot new Env(
    db = lila.db.Env.current,
    config = lila.common.PlayApp loadConfig "irwin",
    tournamentApi = lila.tournament.Env.current.api,
    modApi = lila.mod.Env.current.api,
    reportApi = lila.report.Env.current.api,
    notifyApi = lila.notify.Env.current.api,
    userCache = lila.user.Env.current.cached,
    settingStore = lila.memo.Env.current.settingStore,
    scheduler = lila.common.PlayApp.scheduler,
    system = lila.common.PlayApp.system
  )
}
