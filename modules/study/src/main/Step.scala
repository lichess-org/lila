package lila.study

import chess.format.Uci

case class Step(
    ply: Int,
    move: Option[Step.Move],
    fen: String,
    check: Boolean,
    variations: List[List[Step]]) {
}

object Step {

  case class Move(uci: Uci, san: String)
}
