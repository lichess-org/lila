package lila
package model

case class Situation(board: Board, color: Color) {

  lazy val actors = board actorsOf color

  lazy val moves: Map[Pos, Set[Pos]] = actors map { actor ⇒
    actor.pos -> actor.moves
  } toMap

  lazy val check: Boolean = board kingPosOf color map { king ⇒
    board actorsOf !color exists (_ threatens king)
  } getOrElse false

  def checkMate: Boolean = check && moves.isEmpty

  def staleMate: Boolean = !check && moves.isEmpty
}
