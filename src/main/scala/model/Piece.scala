package lila
package model

case class Piece(color: Color, role: Role) {

  def basicMoves(pos: Pos, board: Board): Set[Pos] = {

    def friends = board occupation color
    def enemies = board occupation !color

    role match {
      case Pawn ⇒ {
        val dir: Direction = if (color == White) _.up else _.down
        dir(pos) map { next ⇒
          val unmoved = (color == White && pos.y == 2) || (color == Black && pos.y == 7)
          val one = Some(next) filterNot board.occupations
          Set(
            one,
            if (unmoved) one flatMap { o => dir(o) filterNot board.occupations }
            else None,
            next.left filter enemies,
            next.right filter enemies
          ) flatten
        } getOrElse Set.empty
      }
      case r if (r.trajectory) ⇒ (new Trajectories(r.dirs, friends, enemies)) from pos
      case r                   ⇒ (r.dirs map { d ⇒ d(pos) }).flatten.toSet -- friends
    }
  }

  def is(c: Color) = c == color
  def is(r: Role) = r == role

  def forsyth: Char = if (color == White) role.forsyth.toUpper else role.forsyth

  override def toString = (color + "-" + role).toLowerCase
}
