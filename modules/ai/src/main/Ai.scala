package lila.ai

import chess.{ Game, Move }
import lila.analyse.Analysis

trait Ai {

  def play(game: Game, pgn: String, initialFen: Option[String], level: Int): Fu[Valid[(Game, Move)]]

  def analyse(pgn: String, initialFen: Option[String]): Fu[Valid[String ⇒ Analysis]]
}
