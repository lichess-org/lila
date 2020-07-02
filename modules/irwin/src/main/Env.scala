package lila.irwin

import akka.actor._
import com.softwaremill.macwire._
import scala.concurrent.duration._

import lila.common.config._
import lila.tournament.TournamentApi

final class Env(
    tournamentApi: TournamentApi,
    modApi: lila.mod.ModApi,
    reportApi: lila.report.ReportApi,
    notifyApi: lila.notify.NotifyApi,
    userCache: lila.user.Cached,
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    analysisRepo: lila.analyse.AnalysisRepo,
    settingStore: lila.memo.SettingStore.Builder,
    db: lila.db.Db
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  private lazy val reportColl = db(CollName("irwin_report"))

  lazy val irwinThresholdsSetting = IrwinThresholds makeSetting settingStore

  lazy val stream = wire[IrwinStream]

  lazy val api = wire[IrwinApi]

  system.scheduler.scheduleWithFixedDelay(5 minutes, 5 minutes) { () =>
    tournamentApi.allCurrentLeadersInStandard flatMap api.requests.fromTournamentLeaders
  }
  system.scheduler.scheduleWithFixedDelay(15 minutes, 15 minutes) { () =>
    userCache.getTop50Online flatMap api.requests.fromLeaderboard
  }
}
