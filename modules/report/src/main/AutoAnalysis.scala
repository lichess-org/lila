package lila.report

import org.joda.time.DateTime

import lila.game.{ Game, GameRepo }

final class AutoAnalysis(fishnet: akka.actor.ActorSelection) {

  def apply(r: Report): Funit = r.isCheat ?? {
    gamesToAnalyse(r) map { games =>
      if (games.nonEmpty)
        logger.info(s"Auto-analyse ${games.size} games after report ${r.createdBy} -> ${r.user}")
      games foreach { game =>
        lila.mon.cheat.autoAnalysis.reason("Report")()
        fishnet ! lila.hub.actorApi.fishnet.AutoAnalyse(game.id)
      }
    }
  }

  private def gamesToAnalyse(r: Report): Fu[List[Game]] = {
    GameRepo.recentAnalysableGamesByUserId(r.user, 10) |+|
      GameRepo.lastGamesBetween(r.user, r.createdBy, DateTime.now.minusHours(2), 10)
  }.map {
    _.filter { g => g.analysable && !g.metadata.analysed }
      .distinct
      .sortBy(-_.createdAt.getSeconds)
      .take(5)
  }
}
