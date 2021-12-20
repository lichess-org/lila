package lila.irwin

import akka.actor._
import com.softwaremill.macwire._
import com.softwaremill.tagging._
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

  private lazy val irwinReportColl = db(CollName("irwin_report")).taggedWith[IrwinColl]

  lazy val irwinThresholdsSetting = IrwinThresholds makeSetting settingStore

  lazy val irwinStream = wire[IrwinStream]

  lazy val irwinApi = wire[IrwinApi]

  system.scheduler.scheduleWithFixedDelay(5 minutes, 5 minutes) { () =>
    tournamentApi.allCurrentLeadersInStandard.flatMap(irwinApi.requests.fromTournamentLeaders).unit
  }
  system.scheduler.scheduleWithFixedDelay(15 minutes, 15 minutes) { () =>
    userCache.getTop50Online.flatMap(irwinApi.requests.fromLeaderboard).unit
  }

  private lazy val kaladinColl = db(CollName("kaladin_queue")).taggedWith[KaladinColl]

  lazy val kaladinApi = wire[KaladinApi]
}

trait IrwinColl
trait KaladinColl
