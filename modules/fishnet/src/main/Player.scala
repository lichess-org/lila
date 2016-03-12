package lila.fishnet

import org.joda.time.DateTime

import chess.format.{ FEN, Forsyth }

import lila.game.{ Game, GameRepo, UciMemo }

final class Player(
    api: FishnetApi,
    uciMemo: UciMemo,
    sequencer: lila.hub.FutureSequencer) {

  def apply(game: Game): Funit = game.aiLevel ?? { level =>
    makeMove(game, level) flatMap { move =>
      sequencer {
        api similarMoveExists move flatMap {
          _.fold(funit, api addMove move)
        }
      }
    }
  }

  private def makeMove(game: Game, level: Int): Fu[Work.Move] =
    if (game.toChess.situation playable true)
      GameRepo.initialFen(game) zip uciMemo.get(game) map {
        case (fen, moves) => Work.Move(
          _id = Work.makeId,
          game = Work.Game(
            id = game.id,
            position = fen map FEN.apply,
            variant = game.variant,
            moves = moves),
          currentFen = FEN(Forsyth >> game.toChess),
          level = level,
          tries = 0,
          acquired = None,
          createdAt = DateTime.now)
      }
    else fufail("[fishnet] invalid position")
}
