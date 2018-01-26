package lila.fishnet

import org.joda.time.DateTime

import chess.format.FEN
import chess.format.Uci

import lila.analyse.AnalysisRepo
import lila.game.{ Game, GameRepo, UciMemo }

final class Analyser(
    repo: FishnetRepo,
    uciMemo: UciMemo,
    sequencer: lila.hub.FutureSequencer,
    evalCache: FishnetEvalCache,
    limiter: Limiter
) {

  val maxPlies = 200

  def apply(game: Game, sender: Work.Sender): Fu[Boolean] =
    AnalysisRepo exists game.id flatMap {
      case true => fuccess(false)
      case _ if Game.isOldHorde(game) => fuccess(false)
      case _ =>
        limiter(sender) flatMap { accepted =>
          accepted ?? {
            makeWork(game, sender) flatMap { work =>
              sequencer {
                repo getSimilarAnalysis work flatMap {
                  // already in progress, do nothing
                  case Some(similar) if similar.isAcquired => funit
                  // queued by system, reschedule for the human sender
                  case Some(similar) if similar.sender.system && !sender.system =>
                    repo.updateAnalysis(similar.copy(sender = sender))
                  // queued for someone else, do nothing
                  case Some(similar) => funit
                  // first request, store
                  case _ =>
                    lila.mon.fishnet.analysis.requestCount()
                    evalCache skipPositions work.game flatMap { skipPositions =>
                      lila.mon.fishnet.analysis.evalCacheHits(skipPositions.size)
                      repo addAnalysis work.copy(skipPositions = skipPositions)
                    }
                }
              }
            }
          } inject accepted
        }
    }

  def apply(gameId: String, sender: Work.Sender): Fu[Boolean] =
    GameRepo game gameId flatMap { _ ?? { apply(_, sender) } }

  def study(req: lila.hub.actorApi.fishnet.StudyChapterRequest): Fu[Boolean] =
    AnalysisRepo exists req.chapterId flatMap {
      case true => fuccess(false)
      case _ => {
        import req._
        val sender = Work.Sender(req.userId, none, false, false)
        limiter(sender) flatMap { accepted =>
          accepted ?? {
            val work = makeWork(
              game = Work.Game(
                id = chapterId,
                initialFen = initialFen,
                studyId = studyId.some,
                variant = variant,
                moves = moves take maxPlies map (_.uci) mkString " "
              ),
              startPly = 0,
              sender = sender
            )
            sequencer {
              repo getSimilarAnalysis work flatMap {
                _.isEmpty ?? {
                  lila.mon.fishnet.analysis.requestCount()
                  evalCache skipPositions work.game flatMap { skipPositions =>
                    lila.mon.fishnet.analysis.evalCacheHits(skipPositions.size)
                    repo addAnalysis work.copy(skipPositions = skipPositions)
                  }
                }
              }
            }
          } inject accepted
        }
      }
    }

  private def makeWork(game: Game, sender: Work.Sender): Fu[Work.Analysis] =
    GameRepo.initialFen(game) zip uciMemo.get(game) map {
      case (initialFen, moves) => makeWork(
        game = Work.Game(
          id = game.id,
          initialFen = initialFen map FEN.apply,
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
