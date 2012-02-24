package lila
package model

case class Piece(color: Color, role: Role) {

  def basicMoves(pos: Pos, board: Board): Set[Pos] = {

    def friends = board occupation color
    def enemies = board occupation !color

    role match {
      case Pawn ⇒ {
        def unmoved = (color == White && pos.y == 2) || (color == Black && pos.y == 7)
        val dir: Direction = if (color == White) _.up else _.down
        dir(pos) map { one ⇒
          Set(
            Some(one) filterNot friends,
            if (unmoved) dir(one) filterNot friends else None,
            one.left filter enemies,
            one.right filter enemies
          ).flatten
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
