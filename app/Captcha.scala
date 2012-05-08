package lila

import db._
import model._
import chess.{ Game, Color }
import chess.format.{ Forsyth, PgnReader }
import scalaz.effects._
import scalaz.NonEmptyList

// only works with standard chess (not chess960)
final class Captcha(gameRepo: GameRepo) {

  // returns game id and game fen and current player color
  val create: IO[Valid[(String, String, Color)]] =
    gameRepo.findOneStandardCheckmate map { gameOption ⇒
      for {
        game ← gameOption toValid "No checkmate available"
        rewinded ← rewind(game)
      } yield (game.id, fen(rewinded), rewinded.player)
    }

  def solve(id: String): IO[Valid[NonEmptyList[String]]] =
    gameRepo game id map { gameOption ⇒
      for {
        game ← gameOption toValid "No such game"
        rewinded ← rewind(game)
        moves ← mateMoves(rewinded).toNel toValid "No solution found"
      } yield moves
    }

  private def mateMoves(game: Game): List[String] =
    game.situation.moves.toList flatMap {
      case (_, moves) ⇒ moves filter { move ⇒
        (move.after situationOf !game.player).checkMate
      }
    } map (_.notation)

  private def rewind(game: DbGame): Valid[Game] = 
    PgnReader.withSans(game.pgn, _.init) map (_.game) mapFail failInfo(game)

  private def fen(game: Game): String = Forsyth >> game takeWhile (_ != ' ')

  private def failInfo(game: DbGame) =
    (failures: Failures) ⇒ "Rewind %s".format(game.id) <:: failures
}
