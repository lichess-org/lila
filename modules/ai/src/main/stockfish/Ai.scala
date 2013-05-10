package lila.ai
package stockfish

import chess.{ Game, Move }
import lila.analyse.Analysis

final class Ai(server: Server) extends lila.ai.Ai {

  import model._

  def play(game: Game, pgn: String, initialFen: Option[String], level: Int): Fu[(Game, Move)] =
    withValidSituation(game) {
      server.play(pgn, initialFen, level) flatMap { 
        Stockfish.applyMove(game, pgn, _)
      }
    } 

  def analyse(pgn: String, initialFen: Option[String]): Fu[String ⇒ Analysis] =
    server.analyse(pgn, initialFen)

  private def withValidSituation[A](game: Game)(op: ⇒ Fu[A]): Fu[A] =
    if (game.situation playable true) op
    else fufail("[ai stockfish] invalid game situation: " + game.situation)
}
