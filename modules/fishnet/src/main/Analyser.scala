package lila.fishnet

import chess.Ply
import lila.analyse.AnalysisRepo
import lila.game.{ Game, UciMemo }
import lila.common.config.Max
import lila.analyse.Analysis

final class Analyser(
    repo: FishnetRepo,
    analysisRepo: AnalysisRepo,
    gameRepo: lila.game.GameRepo,
    uciMemo: UciMemo,
    evalCache: FishnetEvalCache,
    limiter: FishnetLimiter
)(using Executor, Scheduler):

  val maxPlies = 300

  private val workQueue =
    lila.hub.AsyncActorSequencer(maxSize = Max(256), timeout = 5 seconds, "fishnetAnalyser")

  def apply(game: Game, sender: Work.Sender, ignoreConcurrentCheck: Boolean = false): Fu[Analyser.Result] =
    (game.metadata.analysed so analysisRepo.exists(game.id.value)) flatMap {
      if _ then fuccess(Analyser.Result.AlreadyAnalysed)
      else if !game.analysable then fuccess(Analyser.Result.NotAnalysable)
      else
        limiter(
          sender,
          ignoreConcurrentCheck = ignoreConcurrentCheck,
          ownGame = game.userIds contains sender.userId
        ) flatMap { result =>
          result.ok so {
            makeWork(game, sender) flatMap { work =>
              workQueue:
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
          } inject result
        }
    }

  def apply(gameId: GameId, sender: Work.Sender): Fu[Analyser.Result] =
    gameRepo game gameId flatMap {
      _.fold[Fu[Analyser.Result]](fuccess(Analyser.Result.NoGame))(apply(_, sender))
    }

  def study(req: lila.hub.actorApi.fishnet.StudyChapterRequest): Fu[Analyser.Result] =
    analysisRepo exists req.chapterId.value flatMap {
      if _ then fuccess(Analyser.Result.NoChapter)
      else
        import req.*
        val sender = Work.Sender(req.userId, none, mod = false, system = false)
        (if req.unlimited then fuccess(Analyser.Result.Ok)
         else limiter(sender, ignoreConcurrentCheck = true, ownGame = false)) flatMap { result =>
          if !result.ok then
            logger.info(s"Study request declined: ${req.studyId}/${req.chapterId} by $sender")
          result.ok so {
            val work = makeWork(
              game = Work.Game(
                id = chapterId.value,
                initialFen = initialFen,
                studyId = studyId.some,
                variant = variant,
                moves = moves take maxPlies map (_.uci) mkString " "
              ),
              // if black moves first, use 1 as startPly so the analysis doesn't get reversed
              startPly = Ply(initialFen.map(_.colorOrWhite).so(_.fold(0, 1))),
              sender = sender
            )
            workQueue {
              repo getSimilarAnalysis work flatMap {
                _.isEmpty so {
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
    gameRepo.initialFen(game) zip uciMemo.get(game) map { (initialFen, moves) =>
      makeWork(
        game = Work.Game(
          id = game.id.value,
          initialFen = initialFen,
          studyId = none,
          variant = game.variant,
          moves = moves take maxPlies mkString " "
        ),
        startPly = game.chess.startedAtPly,
        sender = sender
      )
    }

  private def makeWork(game: Work.Game, startPly: Ply, sender: Work.Sender): Work.Analysis =
    Work.Analysis(
      _id = Work.makeId,
      sender = sender,
      game = game,
      startPly = startPly,
      tries = 0,
      lastTryByKey = none,
      acquired = none,
      skipPositions = Nil,
      createdAt = nowInstant
    )

object Analyser:

  enum Result(val error: Option[String]):
    def ok = error.isEmpty
    case Ok                 extends Result(none)
    case NoGame             extends Result("Game not found".some)
    case NoChapter          extends Result("Chapter not found".some)
    case AlreadyAnalysed    extends Result("This game is already analysed".some)
    case NotAnalysable      extends Result("This game is not analysable".some)
    case ConcurrentAnalysis extends Result("You already have an ongoing requested analysis".some)
    case WeeklyLimit        extends Result("You have reached the weekly analysis limit".some)
    case DailyLimit         extends Result("You have reached the daily analysis limit".some)
    case DailyIpLimit       extends Result("You have reached the daily analysis limit on this IP".some)
