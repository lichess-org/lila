package lila.tutor

import akka.stream.scaladsl._

import lila.analyse.Analysis
import lila.analyse.AnalysisRepo
import lila.game.{ Game, GameRepo, Pov, Query }
import lila.user.User

final class TutorReportBuilder(gameRepo: GameRepo, analysisRepo: AnalysisRepo)(implicit
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer
) {

  def apply(user: User): Fu[TutorTimeReport] =
    gameRepo
      .sortedCursor(
        selector = Query.user(user) ++ Query.variantStandard ++ Query.noAi,
        sort = Query.sortAntiChronological
      )
      // .documentSource(10_000)
      // .documentSource(1_000)
      .documentSource(100)
      .mapConcat(Pov.ofUserId(_, user.id).toList)
      .mapAsyncUnordered(16) { pov =>
        analysisRepo.byGame(pov.game).dmap(pov -> _)
      }
      .runWith {
        Sink.fold[TutorTimeReport, (Pov, Option[Analysis])](TutorTimeReport.empty) {
          case (time, (pov, analysis)) => TutorTimeReport.aggregate(time, pov, analysis)
        }
      }
}
