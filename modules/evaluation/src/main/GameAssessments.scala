package lila.evaluation

import chess.Color

case class GameAssessments(
  white: Option[PlayerAssessment],
  black: Option[PlayerAssessment]) {
  def color(c: Color) = c match {
    case Color.White => white
    case _ => black
  }
}
