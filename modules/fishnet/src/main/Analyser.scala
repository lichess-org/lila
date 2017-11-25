package lila.fishnet

import org.joda.time.DateTime

import chess.format.FEN

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
                      repo addAnalysis work // work.copy(skipPositions = skipPositions)
                    }
                }
              }
            }
          } inject accepted
        }
    }

  def apply(gameId: String, sender: Work.Sender): Fu[Boolean] =
    GameRepo game gameId flatMap { _ ?? { apply(_, sender) } }

  private def makeWork(game: Game, sender: Work.Sender): Fu[Work.Analysis] =
    GameRepo.initialFen(game) zip uciMemo.get(game) map {
      case (initialFen, moves) => Work.Analysis(
        _id = Work.makeId,
        sender = sender,
        game = Work.Game(
          id = game.id,
          initialFen = initialFen map FEN.apply,
          variant = game.variant,
          moves = moves.take(maxPlies) mkString " "
        ),
        startPly = game.startedAtTurn,
        nbPly = game.turns,
        tries = 0,
        lastTryByKey = none,
        acquired = none,
        skipPositions = Nil,
        createdAt = DateTime.now
      )
    }
}
