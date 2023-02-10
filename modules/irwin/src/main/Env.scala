package lila.irwin

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*
import play.api.Configuration

import lila.common.config.*
import lila.report.Suspect
import lila.tournament.TournamentApi
import lila.db.dsl.Coll
import lila.db.AsyncColl

final class Env(
    appConfig: Configuration,
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
)(using
    ec: Executor,
    scheduler: Scheduler
):

  lazy val irwinStream = wire[IrwinStream]

  val irwinApi =
    def mk = (coll: Coll) => wire[IrwinApi]
    mk(db(CollName("irwin_report")))

  val kaladinApi =
    def mk = (coll: AsyncColl) => wire[KaladinApi]
    mk(insightDb(CollName("kaladin_queue")))

  if (appConfig.get[Boolean]("kaladin.enabled"))

    scheduler.scheduleWithFixedDelay(5 minutes, 5 minutes) { () =>
      (for {
        leaders <- tournamentApi.allCurrentLeadersInStandard
        suspects <- lila.common.LilaFuture
          .linear(leaders.toList) { case (tour, top) =>
            userRepo byIds top.value.zipWithIndex
              .filter(_._2 <= tour.nbPlayers * 2 / 100)
              .map(_._1.userId)
              .take(20)
          }
          .map(_.flatten.map(Suspect.apply))
        _ <- irwinApi.requests.fromTournamentLeaders(suspects)
        _ <- kaladinApi.tournamentLeaders(suspects)
      } yield ()).unit
    }
    scheduler.scheduleWithFixedDelay(15 minutes, 15 minutes) { () =>
      (for {
        topOnline <- userCache.getTop50Online.map(_ map Suspect.apply)
        _         <- irwinApi.requests.topOnline(topOnline)
        _         <- kaladinApi.topOnline(topOnline)
      } yield ()).unit
    }

    scheduler.scheduleWithFixedDelay(83 seconds, 5 seconds) { () =>
      kaladinApi.readResponses.unit
    }

    scheduler.scheduleWithFixedDelay(1 minute, 1 minute) { () =>
      kaladinApi.monitorQueued.unit
    }
