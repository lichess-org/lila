package lila
package site

import game._
import chess.{ Game, Color }
import chess.format.{ Forsyth, PgnReader }
import scalaz.effects._
import scalaz.NonEmptyList

// only works with standard chess (not chess960)
final class Captcha(gameRepo: GameRepo) {

  import Captcha._

  val create: IO[Valid[Challenge]] =
    gameRepo.findOneStandardCheckmate map { gameOption ⇒
      for {
        game ← gameOption toValid "No checkmate available in db"
        challenge ← get(game)
      } yield challenge 
    }

  def get(id: String): IO[Valid[Challenge]] =
    gameRepo game id map { gameOption ⇒
      for {
        game ← gameOption toValid "No such game: " + id
        challenge ← get(game)
      } yield challenge
    }

  private def get(game: DbGame): Valid[Challenge] = for {
    rewinded ← rewind(game)
    solutions ← solve(rewinded)
  } yield Challenge(game.id, fen(rewinded), rewinded.player, solutions)

  private def solve(game: Game): Valid[Solutions] =
    mateMoves(game).toNel toValid "No solution found for: " + game

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

object Captcha {

  type Solutions = NonEmptyList[String]

  case class Challenge(
      gameId: String,
      fen: String,
      color: Color,
      solutions: Solutions) {

    def valid(solution: String) = solutions.list contains solution
  }
}
