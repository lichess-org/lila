package lila.study

import chess.format.{ Uci, UciCharPair, Forsyth }

case class Node(
    id: UciCharPair,
    ply: Int,
    move: Node.Move,
    fen: String,
    check: Boolean,
    children: List[Node]) {
}

object Node {

  case class Root(
    ply: Int,
    fen: String,
    check: Boolean,
    children: List[Node])

  object Root {

    val default = Root(
      ply = 0,
      fen = Forsyth.initial,
      check = false,
      children = Nil)
  }

  case class Move(uci: Uci, san: String)
}
