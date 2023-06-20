package lila.report

import lila.game.{ Game, GameRepo }

final class AutoAnalysis(
    gameRepo: GameRepo,
    fishnet: lila.hub.actors.Fishnet
)(using
    ec: Executor,
    scheduler: Scheduler
):

  def apply(candidate: Report.Candidate): Funit =
    if (candidate.isCheat) doItNow(candidate)
    else if (candidate.isPrint) fuccess {
      List(30, 90) foreach { minutes =>
        scheduler.scheduleOnce(minutes minutes) { doItNow(candidate).unit }
      }
    }
    else funit

  private def doItNow(candidate: Report.Candidate) =
    gamesToAnalyse(candidate) map { games =>
      if (games.nonEmpty)
        logger.info(s"Auto-analyse ${games.size} games after report by ${candidate.reporter.user.id}")
      games foreach { game =>
        lila.mon.cheat.autoAnalysis("Report").increment()
        fishnet ! lila.hub.actorApi.fishnet.AutoAnalyse(game.id)
      }
    }

  private def gamesToAnalyse(candidate: Report.Candidate): Fu[List[Game]] = {
    gameRepo.recentAnalysableGamesByUserId(candidate.suspect.user.id, 20) flatMap { as =>
      gameRepo.lastGamesBetween(
        candidate.suspect.user,
        candidate.reporter.user,
        nowInstant.minusHours(2),
        10
      ) dmap { as ++ _ }
    }
  }.map {
    _.filter { g =>
      g.analysable && !g.metadata.analysed
    }.distinct
      .sortBy(-_.createdAt.toSeconds)
      .take(10)
  }
