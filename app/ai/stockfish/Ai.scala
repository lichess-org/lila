package lila
package ai
package stockfish

import chess.{ Game, Move }
import game.DbGame
import analyse.Analysis

import scalaz.effects._

final class Ai(server: Server) extends lila.ai.Ai with Stockfish {

  import model._

  def play(dbGame: DbGame, initialFen: Option[String]): IO[Valid[(Game, Move)]] =
    server.play(dbGame.pgn, initialFen, dbGame.aiLevel | 1).fold(
      err ⇒ io(failure(err)),
      iop ⇒ iop map {
        applyMove(dbGame, _)
      }
    )

  def analyse(dbGame: DbGame, initialFen: Option[String]): IO[Valid[Analysis]] =
    server.analyse(dbGame.pgn, initialFen).fold(
      err ⇒ io(failure(err)),
      iop ⇒ iop map success
    )
}
