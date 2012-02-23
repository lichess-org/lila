package lila
package model

case class Piece(color: Color, role: Role) {

  def basicMoves(pos: Pos, board: Board): Set[Pos] = {

    role match {
      case Rook ⇒ {
        val vectors: List[Pos => Option[Pos]] = List(_.up, _.down, _.left, _.right)
        Set.empty
      }
      case _ ⇒ Set.empty
    }
  }

  def is(c: Color) = c == color
  def is(r: Role) = r == role

  def forsyth: Char = if (color == White) role.forsyth.toUpper else role.forsyth

  override def toString = (color + " " + role).toLowerCase
}
