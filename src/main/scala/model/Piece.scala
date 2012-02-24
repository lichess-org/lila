package lila
package model

case class Piece(color: Color, role: Role) {

  def basicMoves(pos: Pos, board: Board): Set[Pos] = {

    val friends = board occupation color
    val enemies = board occupation !color

    role match {
      case r if (r.trajectory) ⇒ (new Trajectories(r.dirs, friends, enemies)) from pos
      case r                   ⇒ (r.dirs map { d ⇒ d(pos) }).flatten.toSet -- friends
    }
  }

  def is(c: Color) = c == color
  def is(r: Role) = r == role

  def forsyth: Char = if (color == White) role.forsyth.toUpper else role.forsyth

  override def toString = (color + "-" + role).toLowerCase
}
