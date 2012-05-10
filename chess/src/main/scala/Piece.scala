package lila.chess

case class Piece(color: Color, role: Role) {

  def is(c: Color) = c == color
  def is(r: Role) = r == role

  def oneOf(rs: Set[Role]) = rs(role)

  def forsyth: Char = if (color == White) role.forsyth.toUpper else role.forsyth

  override def toString = (color + "-" + role).toLowerCase
}
