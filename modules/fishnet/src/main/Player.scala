package lila.fishnet

import org.joda.time.DateTime

import chess.format.FEN

import lila.game.{ Game, GameRepo, UciMemo }

final class Player(
    api: FishnetApi,
    uciMemo: UciMemo) {

  def apply(game: Game): Funit = game.aiLevel ?? { level =>
    makeWork(game, level) flatMap api.addMove
  }

  private def withValidSituation[A](game: Game)(op: => Fu[A]): Fu[A] =
    if (game.toChess.situation playable true) op
    else fufail("[fishnet] invalid position")

  private def makeWork(game: Game, level: Int): Fu[Work.Move] = withValidSituation(game) {
    GameRepo.initialFen(game) zip uciMemo.get(game) map {
      case (fen, moves) => Work.Move(
        _id = Work.makeId,
        game = Work.Game(
          id = game.id,
          position = fen map FEN.apply,
          variant = game.variant,
          moves = moves),
        level = level,
        tries = 0,
        acquired = None,
        createdAt = DateTime.now)
    }
  }
}
