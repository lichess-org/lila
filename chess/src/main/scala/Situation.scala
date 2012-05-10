package lila.chess

case class Situation(board: Board, color: Color) {

  lazy val actors = board actorsOf color

  lazy val moves: Map[Pos, List[Move]] = actors collect {
    case actor if actor.moves.nonEmpty ⇒ actor.pos -> actor.moves
  } toMap

  lazy val destinations: Map[Pos, List[Pos]] = moves mapValues { ms ⇒ ms map (_.dest) }

  lazy val kingPos: Option[Pos] = board kingPosOf color

  lazy val check: Boolean = board check color

  def checkMate: Boolean = check && moves.isEmpty

  def staleMate: Boolean = !check && moves.isEmpty

  def autoDraw: Boolean = board.autoDraw

  def threefoldRepetition: Boolean = board.history.threefoldRepetition

  def end: Boolean = checkMate || staleMate || autoDraw

  def move(from: Pos, to: Pos, promotion: PromotableRole): Valid[Move] = {

    val someMove = for {
      actor ← board.actors get from
      if actor is color
      m ← actor.moves find (_.dest == to)
    } yield m

    if (promotion == Queen) someMove
    else for {
      m ← someMove
      b1 = m.after
      if (b1 count color.queen) > (board count color.queen)
      b2 ← b1 take to
      b3 ← b2.place(color - promotion, to)
    } yield m.copy(after = b3, promotion = Some(promotion))

  } toSuccess "Invalid move %s %s".format(from, to).wrapNel
}

object Situation {

  def apply(): Situation = Situation(Board(), White)
}
