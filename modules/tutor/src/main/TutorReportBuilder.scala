package lila.tutor

import scala.concurrent.duration._
import akka.stream.scaladsl._
import chess.Replay

import lila.analyse.Analysis
import lila.analyse.AnalysisRepo
import lila.db.dsl._
import lila.game.{ Divider, Game, GameRepo, Pov, Query }
import lila.user.User
import org.joda.time.DateTime
import lila.fishnet.Analyser
import lila.fishnet.FishnetAwaiter
import lila.common.IpAddress

final class TutorReportBuilder(
    gameRepo: GameRepo,
    analysisRepo: AnalysisRepo,
    fishnetAnalyser: Analyser,
    fishnetAwaiter: FishnetAwaiter
)(implicit
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer
) {

  import TutorReportBuilder._

  private val maxGames                   = 1000
  private val requireAnalysisOnLastGames = 15

  def apply(user: User, ip: IpAddress): Fu[TutorReport] =
    gameRepo
      .sortedCursor(
        selector = gameSelector(user),
        sort = Query.sortAntiChronological
      )
      .documentSource(maxGames * 3)
      .via(prePovFlow(user))
      .take(maxGames)
      .zipWithIndex
      .mapAsyncUnordered(4) { case (pre, index) =>
        getAnalysis(user, ip, pre.pov.game, index.toInt) map { analysis =>
          RichPov(
            pre.pov,
            pre.perfType,
            pre.replay.toVector,
            analysis,
            chess.Divider(pre.replay.view.map(_.board).toList)
          )
        }
      }
      .runWith {
        Sink.fold[TutorReport, RichPov](TutorReport(user.id, DateTime.now, Map.empty)) { case (report, pov) =>
          TutorReport.aggregate(report, pov)
        }
      }

  private def getAnalysis(user: User, ip: IpAddress, game: Game, index: Int) =
    analysisRepo.byGame(game) orElse {
      (index < requireAnalysisOnLastGames) ?? requestAnalysis(
        game,
        lila.fishnet.Work.Sender(userId = user.id, ip = ip.some, mod = false, system = false)
      )
    }

  private def requestAnalysis(game: Game, sender: lila.fishnet.Work.Sender): Fu[Option[Analysis]] = {
    def fetch = analysisRepo byId game.id
    fishnetAnalyser(game, sender, ignoreConcurrentCheck = true) flatMap {
      case Analyser.Result.Ok              => fishnetAwaiter(game.id, 3 minutes) >> fetch
      case Analyser.Result.AlreadyAnalysed => fetch
      case _                               => fuccess(none)
    }
  }

  private def gameSelector(user: User) =
    Query.user(user) ++ Query.variantStandard ++ Query.noAi // ++ $id("OsPJmvwA")
}

private object TutorReportBuilder {

  def prePovFlow(user: User) = Flow[Game].mapConcat(g => toPrePov(user, g).toList)

  private def toPrePov(user: User, game: Game): Option[PrePov] =
    for {
      perfType <- game.perfType.filter(TutorReport.perfTypeSet.contains)
      pov      <- Pov.ofUserId(game, user.id)
      replay = ~Replay.situations(pov.game.pgnMoves, none, pov.game.variant).toOption
      if replay.sizeIs >= 10
    } yield PrePov(pov, perfType, replay)
}
