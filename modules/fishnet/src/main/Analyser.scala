package lila.fishnet

import org.joda.time.DateTime
import shogi.format.forsyth.Sfen
import scala.concurrent.duration._
import scala.util.chaining._

import lila.analyse.AnalysisRepo
import lila.game.Game

final class Analyser(
    repo: FishnetRepo,
    analysisRepo: AnalysisRepo,
    gameRepo: lila.game.GameRepo,
    evalCache: FishnetEvalCache,
    limiter: Limiter
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

  val maxPlies = 200

  private val workQueue = new lila.hub.DuctSequencer(maxSize = 256, timeout = 5 seconds, "fishnetAnalyser")

  def apply(game: Game, sender: Work.Sender): Fu[Boolean] =
    (game.metadata.analysed ?? analysisRepo.exists(game.id)) flatMap {
      case true => fuFalse
      case _ =>
        limiter(sender, ignoreConcurrentCheck = false) flatMap { accepted =>
          accepted ?? {
            makeWork(game, sender) pipe { work =>
              workQueue {
                repo getSimilarAnalysis work flatMap {
                  // already in progress, do nothing
                  case Some(similar) if similar.isAcquired => funit
                  // queued by system, reschedule for the human sender
                  case Some(similar) if similar.sender.system && !sender.system =>
                    repo.updateAnalysis(similar.copy(sender = sender))
                  // queued for someone else, do nothing
                  case Some(_) => funit
                  // first request, store
                  case _ =>
                    lila.mon.fishnet.analysis.requestCount("game").increment()
                    repo addAnalysis work
                  //evalCache skipPositions work.game flatMap { skipPositions =>
                  //  lila.mon.fishnet.analysis.evalCacheHits.record(skipPositions.size)
                  //  repo addAnalysis work.copy(skipPositions = skipPositions)
                  //}
                }
              }
            }
          } inject accepted
        }
    }

  def apply(gameId: String, sender: Work.Sender): Fu[Boolean] =
    gameRepo game gameId flatMap { _ ?? { apply(_, sender) } }

  def study(req: lila.hub.actorApi.fishnet.StudyChapterRequest): Fu[Boolean] =
    analysisRepo exists req.chapterId flatMap {
      case true => fuFalse
      case _ => {
        import req._
        val sender = Work.Sender(req.userId.some, none, false, system = false)
        limiter(sender, ignoreConcurrentCheck = true) flatMap { accepted =>
          if (!accepted) logger.info(s"Study request declined: ${req.studyId}/${req.chapterId} by $sender")
          accepted ?? {
            val work = makeWork(
              game = Work.Game(
                id = chapterId,
                initialSfen = initialSfen,
                studyId = studyId.some,
                variant = variant,
                moves = moves take maxPlies map (_.usi) mkString " "
              ),
              // if gote moves first, use 1 as startPly so the analysis doesn't get reversed
              startPly = initialSfen.flatMap(_.color).??(_.fold(0, 1)),
              sender = sender
            )
            workQueue {
              repo getSimilarAnalysis work flatMap {
                _.isEmpty ?? {
                  lila.mon.fishnet.analysis.requestCount("study").increment()
                  repo addAnalysis work
                  //evalCache skipPositions work.game flatMap { skipPositions =>
                  //  lila.mon.fishnet.analysis.evalCacheHits.record(skipPositions.size)
                  //  repo addAnalysis work.copy(skipPositions = skipPositions)
                  //}
                }
              }
            }
          } inject accepted
        }
      }
    }

  private def makeWork(game: Game, sender: Work.Sender): Work.Analysis =
    makeWork(
      game = Work.Game(
        id = game.id,
        initialSfen = game.initialSfen,
        studyId = none,
        variant = game.variant,
        moves = game.usiMoves.take(maxPlies).map(_.usi).mkString(" ")
      ),
      startPly = game.shogi.startedAtPly,
      sender = sender
    )

  private def makeWork(game: Work.Game, startPly: Int, sender: Work.Sender): Work.Analysis =
    Work.Analysis(
      _id = Work.makeId,
      sender = sender,
      game = game,
      startPly = startPly,
      tries = 0,
      lastTryByKey = none,
      acquired = none,
      skipPositions = Nil,
      createdAt = DateTime.now
    )
}
