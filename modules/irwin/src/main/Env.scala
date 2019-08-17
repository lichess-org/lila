package lidraughts.irwin

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lidraughts.tournament.TournamentApi

final class Env(
    config: Config,
    system: ActorSystem,
    scheduler: lidraughts.common.Scheduler,
    tournamentApi: TournamentApi,
    modApi: lidraughts.mod.ModApi,
    reportApi: lidraughts.report.ReportApi,
    notifyApi: lidraughts.notify.NotifyApi,
    userCache: lidraughts.user.Cached,
    settingStore: lidraughts.memo.SettingStore.Builder,
    db: lidraughts.db.Env
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
    bus = system.lidraughtsBus,
    mode = irwinModeSetting.get
  )

  scheduler.future(5 minutes, "irwin tournament leaders") {
    tournamentApi.allCurrentLeadersInStandard flatMap api.requests.fromTournamentLeaders
  }
  scheduler.future(15 minutes, "irwin leaderboards") {
    api.requests fromLeaderboard userCache.getTop50Online
  }
}

object Env {

  lazy val current: Env = "irwin" boot new Env(
    db = lidraughts.db.Env.current,
    config = lidraughts.common.PlayApp loadConfig "irwin",
    tournamentApi = lidraughts.tournament.Env.current.api,
    modApi = lidraughts.mod.Env.current.api,
    reportApi = lidraughts.report.Env.current.api,
    notifyApi = lidraughts.notify.Env.current.api,
    userCache = lidraughts.user.Env.current.cached,
    settingStore = lidraughts.memo.Env.current.settingStore,
    scheduler = lidraughts.common.PlayApp.scheduler,
    system = lidraughts.common.PlayApp.system
  )
}
