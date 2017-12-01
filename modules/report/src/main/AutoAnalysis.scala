package lila.report

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.game.{ Game, GameRepo }

final class AutoAnalysis(
    fishnet: akka.actor.ActorSelection,
    system: akka.actor.ActorSystem
) {

  def apply(r: Report): Funit =
    if (r.isCheat) doItNow(r)
    else if (r.isPrint) fuccess {
      List(30, 90) foreach { minutes =>
        system.scheduler.scheduleOnce(minutes minutes) { doItNow(r) }
      }
    }
    else funit

  private def doItNow(r: Report) =
    gamesToAnalyse(r) map { games =>
      if (games.nonEmpty)
        logger.info(s"Auto-analyse ${games.size} games after report ${r.lastAtom.at} -> ${r.user}")
      games foreach { game =>
        lila.mon.cheat.autoAnalysis.reason("Report")()
        fishnet ! lila.hub.actorApi.fishnet.AutoAnalyse(game.id)
      }
    }

  private def gamesToAnalyse(r: Report): Fu[List[Game]] = {
    GameRepo.recentAnalysableGamesByUserId(r.user, 10) |+|
      GameRepo.lastGamesBetween(r.user, r.lastAtom.by.userId, DateTime.now.minusHours(2), 10)
  }.map {
    _.filter { g => g.analysable && !g.metadata.analysed }
      .distinct
      .sortBy(-_.createdAt.getSeconds)
      .take(5)
  }
}
