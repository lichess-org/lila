package lidraughts.report

import org.joda.time.DateTime
import scala.concurrent.duration._

import lidraughts.game.{ Game, GameRepo }

final class AutoAnalysis(
    draughtsnet: akka.actor.ActorSelection,
    system: akka.actor.ActorSystem
) {

  def apply(candidate: Report.Candidate): Funit =
    if (candidate.isCheat) doItNow(candidate)
    else if (candidate.isPrint) fuccess {
      List(30, 90) foreach { minutes =>
        system.scheduler.scheduleOnce(minutes minutes) { doItNow(candidate) }
      }
    }
    else funit

  private def doItNow(candidate: Report.Candidate) =
    gamesToAnalyse(candidate) map { games =>
      if (games.nonEmpty)
        logger.info(s"Auto-analyse ${games.size} games of ${candidate.suspect.user.id} after report by ${candidate.reporter.user.id}")
      games foreach { game =>
        lidraughts.mon.cheat.autoAnalysis.reason("Report")()
        draughtsnet ! lidraughts.hub.actorApi.draughtsnet.AutoAnalyse(game.id)
      }
    }

  private def gamesToAnalyse(candidate: Report.Candidate): Fu[List[Game]] = {
    GameRepo.recentAnalysableGamesByUserId(candidate.suspect.user.id, 10) |+|
      GameRepo.lastGamesBetween(candidate.suspect.user, candidate.reporter.user, DateTime.now.minusHours(2), 10)
  }.map {
    _.filter { g => g.analysable && !g.metadata.analysed }
      .distinct
      .sortBy(-_.createdAt.getSeconds)
      .take(5)
  }
}
