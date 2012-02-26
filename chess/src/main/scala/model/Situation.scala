package lila.chess
package model

case class Situation(board: Board, color: Color) {

  lazy val actors = board actorsOf color

  lazy val moves: Map[Pos, Set[Pos]] = actors collect {
    case actor if actor.moves.nonEmpty ⇒ actor.pos -> actor.moves
  } toMap

  lazy val check: Boolean = board kingPosOf color map { king ⇒
    board actorsOf !color exists (_ threatens king)
  } getOrElse false

  def checkMate: Boolean = check && moves.isEmpty

  def staleMate: Boolean = !check && moves.isEmpty

  def playMove(from: Pos, to: Pos): Valid[Situation] = {
    for {
      actor ← board.actors get from
      if actor is color
      newBoard ← actor.implications get to
    } yield newBoard as !color
  } toSuccess "Invalid move %s->%s".format(from, to).wrapNel

  def playMove(move: (Pos, Pos)): Valid[Situation] = playMove(move._1, move._2)

  def playMoves(moves: (Pos, Pos)*): Valid[Situation] =
    moves.foldLeft(success(this): Valid[Situation]) { (sit, move) ⇒
      sit flatMap (_ playMove move)
    }

  def as(c: Color) = copy(color = c)
}

object Situation {

  def apply(): Situation = Situation(Board(), White)
}
