package lila.report

import lila.core.game.{ GameApi, GameRepo }

final class AutoAnalysis(gameRepo: GameRepo, gameApi: GameApi)(using ec: Executor, scheduler: Scheduler):

  def apply(candidate: Report.Candidate): Funit =
    if candidate.isCheat then doItNow(candidate)
    else
      (candidate.reason == Reason.AltPrint).so(fuccess:
        List(30, 90).foreach: minutes =>
          scheduler.scheduleOnce(minutes.minutes)(doItNow(candidate)))

  private def doItNow(candidate: Report.Candidate) =
    gamesToAnalyse(candidate).map: games =>
      if games.nonEmpty then
        logger.info(s"Auto-analyse ${games.size} games after report by ${candidate.reporter.user.id}")
      games.foreach: game =>
        lila.mon.cheat.autoAnalysis("Report").increment()
        lila.common.Bus.pub(lila.core.fishnet.Bus.GameRequest(game.id))

  private def gamesToAnalyse(candidate: Report.Candidate): Fu[List[Game]] =
    gameRepo
      .recentAnalysableGamesByUserId(candidate.suspect.user.id, 20)
      .flatMap: as =>
        gameRepo
          .lastGamesBetween(
            candidate.suspect.user,
            candidate.reporter.user,
            nowInstant.minusHours(2),
            10
          )
          .dmap { as ++ _ }
      .map:
        _.filter: g =>
          gameApi.analysable(g) && !g.metadata.analysed
        .distinct
          .sortBy(-_.createdAt.toSeconds)
          .take(10)
