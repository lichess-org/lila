package lila.tutor

import akka.stream.scaladsl._
import chess.Replay

import lila.analyse.Analysis
import lila.analyse.AnalysisRepo
import lila.db.dsl._
import lila.game.{ Divider, Game, GameRepo, Pov, Query }
import lila.user.User

final class TutorReportBuilder(gameRepo: GameRepo, analysisRepo: AnalysisRepo, divider: Divider)(implicit
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer
) {

  def apply(user: User): Fu[TutorTimeReport] =
    gameRepo
      .sortedCursor(
        selector = Query.user(user) ++ Query.variantStandard ++ Query.noAi ++ $id("OsPJmvwA"),
        sort = Query.sortAntiChronological
      )
      // .documentSource(10_000)
      // .documentSource(1_000)
      .documentSource(1000)
      .mapConcat(Pov.ofUserId(_, user.id).toList)
      .mapAsyncUnordered(16) { pov =>
        analysisRepo.byGame(pov.game) map { analysis =>
          RichPov(
            pov,
            analysis,
            divider.noCache(pov.game.id, pov.game.pgnMoves, pov.game.variant, none),
            Replay.situations(pov.game.pgnMoves, none, pov.game.variant).toOption.??(_.toVector)
          )
        }
      }
      .runWith {
        Sink.fold[TutorTimeReport, RichPov](TutorTimeReport.empty) { case (time, pov) =>
          TutorTimeReport.aggregate(time, pov)
        }
      }
}
