package lila
package model

case class Actor(piece: Piece, pos: Pos, board: Board) {

  def moves: Set[Pos] = piece.role match {
    case Pawn ⇒ {
      dir(pos) map { next ⇒
        val unmoved = if (color == White) pos.y == 2 else pos.y == 7
        val passable = if (color == White) pos.y == 5 else pos.y == 4
        val one = Some(next) filterNot board.occupations
        def enpassant(horizontal: Direction): Option[Pos] = for {
          victimPos ← horizontal(pos)
          victim ← board(victimPos)
          if victim == !color - Pawn
          targetPos ← horizontal(next)
          victimFrom ← dir(victimPos) flatMap dir
          if board.history.lastMove == Some(victimFrom, victimPos)
        } yield targetPos
        Set(
          one,
          if (unmoved) one flatMap { o ⇒ dir(o) filterNot board.occupations }
          else None,
          next.left filter enemies,
          next.right filter enemies,
          if (passable) enpassant(_.left) else None,
          if (passable) enpassant(_.right) else None
        ) flatten
      } getOrElse Set.empty
    }
    case r if (r.trajectory) ⇒ (new Trajectories(r.dirs, friends, enemies)) from pos
    case r                   ⇒ (r.dirs map { d ⇒ d(pos) }).flatten.toSet -- friends
  }

  // does it threaten the opponent king?
  def threatens(to: Pos): Boolean =
    if (enemies(to)) {
      val poss: Set[Pos] = piece.role match {
        case Pawn ⇒ dir(pos) map { next ⇒
          Set(next.left, next.right) flatten
        } getOrElse Set.empty
        case r if (r.trajectory) ⇒ {
          new Trajectories(r.dirs, board occupation color, board occupation !color)
        } from pos
        case r if (r.threatens) ⇒ (r.dirs map { d ⇒ d(pos) }).flatten.toSet
        case _                  ⇒ Set()
      }
      poss(to)
    }
    else false

  def color = piece.color
  def is(c: Color) = c == piece.color
  def friends = board occupation color
  def enemies = board occupation !color
  def dir: Direction = if (color == White) _.up else _.down
}
