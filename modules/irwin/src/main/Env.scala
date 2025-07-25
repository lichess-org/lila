package lila.irwin

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*
import play.api.Configuration

import lila.core.config.*
import lila.db.AsyncColl
import lila.db.dsl.Coll
import lila.report.Suspect

final class Env(
    appConfig: Configuration,
    tournamentApi: lila.core.tournament.TournamentApi,
    modApi: lila.core.mod.ModApi,
    reportApi: lila.report.ReportApi,
    notifyApi: lila.core.notify.NotifyApi,
    userCache: lila.core.user.CachedApi,
    gameRepo: lila.game.GameRepo,
    userApi: lila.core.user.UserApi,
    analysisRepo: lila.analyse.AnalysisRepo,
    settingStore: lila.memo.SettingStore.Builder,
    cacheApi: lila.memo.CacheApi,
    insightApi: lila.game.core.insight.InsightApi,
    db: lila.db.Db,
    insightDb: lila.db.AsyncDb @@ lila.game.core.insight.InsightDb
)(using Executor)(using scheduler: Scheduler):

  lazy val irwinStream = wire[IrwinStream]

  val irwinApi =
    def mk = (coll: Coll) => wire[IrwinApi]
    mk(db(CollName("irwin_report")))

  val kaladinApi =
    def mk = (coll: AsyncColl) => wire[KaladinApi]
    mk(insightDb(CollName("kaladin_queue")))

  if appConfig.get[Boolean]("kaladin.enabled") then

    scheduler.scheduleWithFixedDelay(5.minutes, 5.minutes): () =>
      (for
        leaders <- tournamentApi.allCurrentLeadersInStandard
        suspects <-
          leaders.toList
            .traverse: (tour, top) =>
              userApi.byIds(
                top.view.zipWithIndex
                  .filter(_._2 <= tour.nbPlayers * 2 / 100)
                  .map(_._1)
                  .take(20)
              )
            .map(_.flatten.map(Suspect.apply))
        _ <- irwinApi.requests.fromTournamentLeaders(suspects)
        _ <- kaladinApi.tournamentLeaders(suspects)
      yield ())
    scheduler.scheduleWithFixedDelay(15.minutes, 15.minutes): () =>
      (for
        topOnline <- userCache.getTop50Online
        suspects = topOnline.map(_.user).map(Suspect.apply)
        _ <- irwinApi.requests.topOnline(suspects)
        _ <- kaladinApi.topOnline(suspects)
      yield ())

    scheduler.scheduleWithFixedDelay(83.seconds, 5.seconds): () =>
      kaladinApi.readResponses

    scheduler.scheduleWithFixedDelay(1.minute, 1.minute): () =>
      kaladinApi.monitorQueued
