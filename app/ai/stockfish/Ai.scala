package lila
package ai
package stockfish

import chess.{ Game, Move }
import game.DbGame

import scalaz.effects._

final class Ai(server: Server) extends lila.ai.Ai with Stockfish {

  import model._

  def apply(dbGame: DbGame, initialFen: Option[String]): IO[Valid[(Game, Move)]] =
    server(dbGame.pgn, initialFen, dbGame.aiLevel | 1).fold(
      err ⇒ io(failure(err)),
      iop ⇒ iop map {
        applyMove(dbGame, _)
      }
    )
}
