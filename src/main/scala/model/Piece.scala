package lila
package model

class Trajectories(dirs: List[Direction], friends: Set[Pos], enemies: Set[Pos]) {

  def from(p: Pos): Set[Pos] = dirs flatMap { dir ⇒ forward(p, dir) } toSet

  private def forward(p: Pos, dir: Direction): List[Pos] = dir(p) match {
    case None                        ⇒ Nil
    case Some(next) if friends(next) ⇒ Nil
    case Some(next) if enemies(next) ⇒ List(next)
    case Some(next)                  ⇒ next :: forward(next, dir)
  }
}

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

  override def toString = (color + " " + role).toLowerCase
}
