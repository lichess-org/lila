package lila.ai

import chess.{ Game, Move }
import lila.analyse.AnalysisMaker

trait Ai {

  def play(game: Game, pgn: String, initialFen: Option[String], level: Int): Fu[(Game, Move)]

  def analyse(pgn: String, initialFen: Option[String]): Fu[AnalysisMaker]

  def load: Fu[Option[Int]]
}
