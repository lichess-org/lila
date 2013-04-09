package lila.ai
package stockfish

import chess.{ Game, Move }
import lila.analyse.Analysis

import play.api.libs.concurrent.Execution.Implicits._

final class Ai(server: Server) extends lila.ai.Ai {

  import model._

  def play(game: Game, pgn: String, initialFen: Option[String], level: Int): Fu[Valid[(Game, Move)]] =
    withValidSituation(game) {
      server.play(pgn, initialFen, level) map { validMove ⇒
        validMove flatMap { Stockfish.applyMove(game, pgn, _) }
      }
    }

  def analyse(pgn: String, initialFen: Option[String]): Fu[Valid[String ⇒ Analysis]] =
    server.analyse(pgn, initialFen)

  private def withValidSituation[A](game: Game)(op: ⇒ Fu[Valid[A]]): Fu[Valid[A]] =
    if (game.situation playable true) op
    else fuccess(!!("Invalid game situation: " + game.situation))
}
