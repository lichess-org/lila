package lila

import db._
import model._
import chess.{ Game, Color }
import chess.format.Forsyth
import scalaz.effects._
import scalaz.NonEmptyList

final class Captcha(gameRepo: GameRepo) {

  // returns game id and game fen and current player color
  val create: IO[Valid[(String, String, Color)]] =
    gameRepo.findOneCheckmate map { gameOption ⇒
      for {
        game ← gameOption toValid "No checkmate available"
        chess = game.toChess
        rewinded ← rewind(chess)
      } yield (game.id, fen(rewinded), !chess.player)
    }

  def solve(id: String): IO[Valid[NonEmptyList[String]]] =
    gameRepo game id map { gameOption ⇒
      for {
        game ← gameOption toValid "No such game"
        moves ← mateMoves(game.toChess).toNel toValid "No solution found"
      } yield moves
    }

  private def mateMoves(game: Game): List[String] =
    game.situation.moves.toList flatMap {
      case (_, moves) ⇒ moves filter { move ⇒
        (move.after situationOf !game.player).checkMate
      }
    } map (_.notation)

  private def rewind(game: Game): Valid[Game] = for {
    lastMove ← game.board.history.lastMove toValid "No last move"
    (orig, dest) = lastMove
    rewindedBoard ← game.board.move(dest, orig) toValid "Can't rewind board"
  } yield game withBoard rewindedBoard

  private def fen(game: Game): String = Forsyth >> game takeWhile (_ != ' ')
}
