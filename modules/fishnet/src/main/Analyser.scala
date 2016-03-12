package lila.fishnet

import org.joda.time.DateTime

import chess.format.{ FEN, Forsyth }

import lila.game.{ Game, GameRepo, UciMemo }

final class Analyser(
    api: FishnetApi,
    uciMemo: UciMemo,
    sequencer: Sequencer,
    limiter: Limiter) {

  def apply(game: Game, sender: Work.Sender): Fu[Boolean] =
    limiter(sender) flatMap { accepted =>
      accepted ?? {
        makeWork(game, sender) flatMap { work =>
          sequencer.analysis {
            api.repo getSimilarAnalysis work flatMap {
              // already in progress, do nothing
              case Some(similar) if similar.isAcquired => funit
              // queued by system, reschedule for the human sender
              case Some(similar) if similar.sender.system && !sender.system =>
                api.repo.updateAnalysis(similar.copy(sender = sender))
              // queued for someone else, do nothing
              case Some(similar) => funit
              // first request, store
              case _             => api.repo addAnalysis work
            }
          }
        }
      } inject accepted
    }

  private def makeWork(game: Game, sender: Work.Sender): Fu[Work.Analysis] =
    GameRepo.initialFen(game) zip uciMemo.get(game) map {
      case (initialFen, moves) => Work.Analysis(
        _id = Work.makeId,
        sender = sender,
        game = Work.Game(
          id = game.id,
          initialFen = initialFen map FEN.apply,
          variant = game.variant,
          moves = moves mkString " "),
        startPly = game.startedAtTurn,
        nbPly = game.turns,
        tries = 0,
        acquired = None,
        createdAt = DateTime.now)
    }
}
