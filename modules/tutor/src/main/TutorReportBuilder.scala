package lila.tutor

import akka.stream.scaladsl._
import chess.Replay
import com.softwaremill.tagging._
import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.analyse.Analysis
import lila.analyse.AnalysisRepo
import lila.common.IpAddress
import lila.db.dsl._
import lila.fishnet.{ Analyser, FishnetAwaiter }
import lila.game.{ Divider, Game, GameRepo, Pov, Query }
import lila.user.User

final class TutorReportBuilder(
    gameRepo: GameRepo,
    analysisRepo: AnalysisRepo,
    fishnetAnalyser: Analyser,
    fishnetAwaiter: FishnetAwaiter,
    reportColl: Coll @@ ReportColl
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem,
    mode: play.api.Mode,
    mat: akka.stream.Materializer
) {

  import TutorReportBuilder._
  import TutorBsonHandlers._

  private val maxGames                   = 1000
  private val requireAnalysisOnLastGames = 15
  private val timeToWaitForAnalysis      = 1 second
  // private val timeToWaitForAnalysis      = 3 minutes

  private val sequencer = new lila.hub.AskPipelines[(User.ID, IpAddress), TutorFullReport](
    compute = getOrCompute,
    expiration = 1 hour,
    timeout = 1 hour,
    name = "tutor.fullReport"
  )

  def apply(user: User, ip: IpAddress): Fu[TutorFullReport] = sequencer(user.id -> ip)

  private def getOrCompute(key: (User.ID, IpAddress)): Fu[TutorFullReport] = key match {
    case (userId, ip) =>
      for {
        previous <- reportColl.find($doc("user" -> userId)).sort($sort desc "at").one[TutorFullReport]
        report <- previous match {
          case Some(p) if p.isFresh => fuccess(p)
          case prev =>
            for {
              newReport <- compute(userId, ip, prev)
              _         <- reportColl.insert.one(newReport)
            } yield newReport
        }
      } yield report
  }

  private def compute(
      userId: User.ID,
      ip: IpAddress,
      previous: Option[TutorFullReport]
  ): Fu[TutorFullReport] =
    gameRepo
      .sortedCursor(
        selector = gameSelector(userId) ++ previous.map(_.at).??(Query.createdSince),
        sort = Query.sortAntiChronological
      )
      .documentSource(maxGames * 3)
      .via(prePovFlow(userId))
      .take(maxGames)
      .zipWithIndex
      .mapAsyncUnordered(4) { case (pre, index) =>
        getAnalysis(userId, ip, pre.pov.game, index.toInt) map { analysis =>
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
        Sink.fold[TutorFullReport, RichPov](TutorFullReport(userId, DateTime.now, Map.empty)) {
          case (report, pov) => TutorFullReport.aggregate(report, pov)
        }
      }

  private def getAnalysis(userId: User.ID, ip: IpAddress, game: Game, index: Int) =
    analysisRepo.byGame(game) orElse {
      (index < requireAnalysisOnLastGames) ?? requestAnalysis(
        game,
        lila.fishnet.Work.Sender(userId = userId, ip = ip.some, mod = false, system = false)
      )
    }

  private def requestAnalysis(game: Game, sender: lila.fishnet.Work.Sender): Fu[Option[Analysis]] = {
    def fetch = analysisRepo byId game.id
    fishnetAnalyser(game, sender, ignoreConcurrentCheck = true) flatMap {
      case Analyser.Result.Ok              => fishnetAwaiter(game.id, timeToWaitForAnalysis) >> fetch
      case Analyser.Result.AlreadyAnalysed => fetch
      case _                               => fuccess(none)
    }
  }

  private def gameSelector(userId: User.ID) =
    Query.user(userId) ++ Query.variantStandard ++ Query.noAi // ++ $id("OsPJmvwA")
}

private object TutorReportBuilder {

  def prePovFlow(userId: User.ID) = Flow[Game].mapConcat(g => toPrePov(userId, g).toList)

  private def toPrePov(userId: User.ID, game: Game): Option[PrePov] =
    for {
      perfType <- game.perfType.filter(TutorFullReport.perfTypeSet.contains)
      pov      <- Pov.ofUserId(game, userId)
      replay = ~Replay.situations(pov.game.pgnMoves, none, pov.game.variant).toOption
      if replay.sizeIs >= 10
    } yield PrePov(pov, perfType, replay)
}
