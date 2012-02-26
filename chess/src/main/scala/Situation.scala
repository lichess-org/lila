package lila.chess

case class Situation(board: Board, color: Color) {

  lazy val actors = board actorsOf color

  lazy val moves: Map[Pos, List[Move]] = actors collect {
    case actor if actor.moves.nonEmpty ⇒ actor.pos -> actor.moves
  } toMap

  lazy val check: Boolean = board kingPosOf color map { king ⇒
    board actorsOf !color exists (_ threatens king)
  } getOrElse false

  def checkMate: Boolean = check && moves.isEmpty

  def staleMate: Boolean = !check && moves.isEmpty

  def playMove(from: Pos, to: Pos, promotion: PromotableRole): Valid[Move] = {

    val move = for {
      actor ← board.actors get from
      if actor is color
      m ← actor.moves find (_.dest == to)
    } yield m

    if (promotion == Queen) move
    else for {
      m ← move
      b1 = m.after
      if (b1 count color.queen) > (board count color.queen)
      b2 ← b1 take to
      b3 ← b2.place(color - promotion, to)
    } yield m.copy(after = b3, promotion = Some(promotion))

  } toSuccess "Invalid move %s->%s".format(from, to).wrapNel

  def as(c: Color) = copy(color = c)
}

object Situation {

  def apply(): Situation = Situation(Board(), White)
}
