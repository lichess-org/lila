package lila.fishnet

import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.analyse.AnalysisRepo
import lila.game.{ Game, UciMemo }

final class Analyser(
    repo: FishnetRepo,
    analysisRepo: AnalysisRepo,
    gameRepo: lila.game.GameRepo,
    uciMemo: UciMemo,
    evalCache: FishnetEvalCache,
    limiter: FishnetLimiter
)(implicit
    ec: scala.concurrent.ExecutionContext,
    scheduler: akka.actor.Scheduler
) {

  val maxPlies = 300

  private val workQueue =
    new lila.hub.AsyncActorSequencer(maxSize = 256, timeout = 5 seconds, "fishnetAnalyser")

  def apply(game: Game, sender: Work.Sender, ignoreConcurrentCheck: Boolean = false): Fu[Analyser.Result] =
    (game.metadata.analysed ?? analysisRepo.exists(game.id)) flatMap {
      case true                  => fuccess(Analyser.Result.AlreadyAnalysed)
      case _ if !game.analysable => fuccess(Analyser.Result.NotAnalysable)
      case _ =>
        limiter(
          sender,
          ignoreConcurrentCheck = ignoreConcurrentCheck,
          ownGame = game.userIds contains sender.userId
        ) flatMap { result =>
          result.ok ?? {
            makeWork(game, sender) flatMap { work =>
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
                    evalCache skipPositions work.game flatMap { skipPositions =>
                      lila.mon.fishnet.analysis.evalCacheHits.record(skipPositions.size)
                      repo addAnalysis work.copy(skipPositions = skipPositions)
                    }
                }
              }
            }
          } inject result
        }
    }

  def apply(gameId: String, sender: Work.Sender): Fu[Analyser.Result] =
    gameRepo game gameId flatMap {
      _.fold[Fu[Analyser.Result]](fuccess(Analyser.Result.NoGame))(apply(_, sender))
    }

  def study(req: lila.hub.actorApi.fishnet.StudyChapterRequest): Fu[Analyser.Result] =
    analysisRepo exists req.chapterId flatMap {
      case true => fuccess(Analyser.Result.NoChapter)
      case _ =>
        import req._
        val sender = Work.Sender(req.userId, none, mod = false, system = false)
        (if (req.unlimited) fuccess(Analyser.Result.Ok)
         else limiter(sender, ignoreConcurrentCheck = true, ownGame = false)) flatMap { result =>
          if (!result.ok) logger.info(s"Study request declined: ${req.studyId}/${req.chapterId} by $sender")
          result.ok ?? {
            val work = makeWork(
              game = Work.Game(
                id = chapterId,
                initialFen = initialFen,
                studyId = studyId.some,
                variant = variant,
                moves = moves take maxPlies map (_.uci) mkString " "
              ),
              // if black moves first, use 1 as startPly so the analysis doesn't get reversed
              startPly = initialFen.flatMap(_.color).??(_.fold(0, 1)),
              sender = sender
            )
            workQueue {
              repo getSimilarAnalysis work flatMap {
                _.isEmpty ?? {
                  lila.mon.fishnet.analysis.requestCount("study").increment()
                  evalCache skipPositions work.game flatMap { skipPositions =>
                    lila.mon.fishnet.analysis.evalCacheHits.record(skipPositions.size)
                    repo addAnalysis work.copy(skipPositions = skipPositions)
                  }
                }
              }
            }
          } inject result
        }
    }

  private def makeWork(game: Game, sender: Work.Sender): Fu[Work.Analysis] =
    gameRepo.initialFen(game) zip uciMemo.get(game) map { case (initialFen, moves) =>
      makeWork(
        game = Work.Game(
          id = game.id,
          initialFen = initialFen,
          studyId = none,
          variant = game.variant,
          moves = moves take maxPlies mkString " "
        ),
        startPly = game.chess.startedAtTurn,
        sender = sender
      )
    }

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

object Analyser {

  sealed abstract class Result(val error: Option[String]) {
    def ok = error.isEmpty
  }
  object Result {
    case object Ok                 extends Result(none)
    case object NoGame             extends Result("Game not found".some)
    case object NoChapter          extends Result("Chapter not found".some)
    case object AlreadyAnalysed    extends Result("This game is already analysed".some)
    case object NotAnalysable      extends Result("This game is not analysable".some)
    case object ConcurrentAnalysis extends Result("You already have an ongoing requested analysis".some)
    case object WeeklyLimit        extends Result("You have reached the weekly analysis limit".some)
    case object DailyLimit         extends Result("You have reached the daily analysis limit".some)
    case object DailyIpLimit       extends Result("You have reached the daily analysis limit on this IP".some)
  }
}
