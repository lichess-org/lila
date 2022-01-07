package lila.irwin

import akka.actor._
import com.softwaremill.macwire._
import com.softwaremill.tagging._
import scala.concurrent.duration._

import lila.common.config._
import lila.tournament.TournamentApi
import lila.report.Suspect

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
    cacheApi: lila.memo.CacheApi,
    insightApi: lila.insight.InsightApi,
    db: lila.db.Db,
    insightDb: lila.db.AsyncDb @@ lila.insight.InsightDb
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  private lazy val irwinReportColl = db(CollName("irwin_report")).taggedWith[IrwinColl]

  lazy val irwinStream = wire[IrwinStream]

  val irwinApi = wire[IrwinApi]

  private lazy val kaladinColl = insightDb(CollName("kaladin_queue")).taggedWith[KaladinColl]

  val kaladinApi = wire[KaladinApi]

  system.scheduler.scheduleWithFixedDelay(5 minutes, 5 minutes) { () =>
    (for {
      leaders <- tournamentApi.allCurrentLeadersInStandard
      suspects <- lila.common.Future
        .linear(leaders.toList) { case (tour, top) =>
          userRepo byIds top.value.zipWithIndex
            .filter(_._2 <= tour.nbPlayers * 2 / 100)
            .map(_._1.userId)
            .take(20)
        }
        .map(_.flatten.map(Suspect))
      _ <- irwinApi.requests.fromTournamentLeaders(suspects)
      _ <- kaladinApi.tournamentLeaders(suspects)
    } yield ()).unit
  }
  system.scheduler.scheduleWithFixedDelay(15 minutes, 15 minutes) { () =>
    (for {
      topOnline <- userCache.getTop50Online.map(_ map Suspect)
      _         <- irwinApi.requests.topOnline(topOnline)
      _         <- kaladinApi.topOnline(topOnline)
    } yield ()).unit
  }

  system.scheduler.scheduleWithFixedDelay(83 seconds, 5 seconds) { () =>
    kaladinApi.readResponses.unit
  }

  system.scheduler.scheduleWithFixedDelay(1 minute, 1 minute) { () =>
    kaladinApi.monitorQueued.unit
  }
}

trait IrwinColl
trait KaladinColl
