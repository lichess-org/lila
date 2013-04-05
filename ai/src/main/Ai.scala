package lila.ai

import chess.{ Game, Move }
// import analyse.Analysis

trait Ai {

  def play(game: Game, pgn: String, initialFen: Option[String]): Fu[Valid[(Game, Move)]]

  // def analyse(pgn: String, initialFen: Option[String]): Fu[Valid[Analysis]]
}
