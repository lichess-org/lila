package draughts

case class Piece(color: Color, role: Role) {

  def is(c: Color) = c == color
  def is(r: Role) = r == role
  def isNot(c: Color) = c != color
  def isNot(r: Role) = r != role

  def oneOf(rs: Set[Role]) = rs(role)

  def isMinor = oneOf(Set(Man))
  def isMajor = oneOf(Set(King))
  def isGhost = role == GhostMan || role == GhostKing

  def forsyth: Char = role.forsyth

  def ghostRole =
    if (isGhost) role
    else if (role == Man) GhostMan
    else GhostKing

  override def toString = (color + "-" + role).toLowerCase
}
