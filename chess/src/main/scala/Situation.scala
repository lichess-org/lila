package lila.chess

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

  def playMoves(moves: (Pos, Pos)*): Valid[Situation] =
    moves.foldLeft(success(this): Valid[Situation]) { (sit, move) ⇒
      sit flatMap { s ⇒ s.playMove(move._1, move._2) }
    }

  def playMove(from: Pos, to: Pos, promotion: PromotableRole = Queen): Valid[Situation] = {

    val newBoard = for {
      actor ← board.actors get from
      if actor is color
      b ← actor.implications get to
    } yield b

    if (promotion == Queen) newBoard map (_ as !color)
    else for {
      b1 ← newBoard
      if (b1 count color.queen) > (board count color.queen)
      b2 ← b1 take to
      b3 ← b2.place(color - promotion, to)
    } yield b3 as !color

  } toSuccess "Invalid move %s->%s".format(from, to).wrapNel

  def as(c: Color) = copy(color = c)
}

object Situation {

  def apply(): Situation = Situation(Board(), White)
}
