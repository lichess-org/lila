package chess

case class Piece(color: Color, role: Role) {

  def is(c: Color)   = c == color
  def is(r: Role)    = r == role
  def isNot(r: Role) = r != role

  def oneOf(rs: Set[Role]) = rs(role)

  def isMinor = oneOf(Set(Knight, Bishop))
  def isMajor = oneOf(Set(Rook))

  def forsyth: Char = if (color == White) role.forsythUpper else role.forsyth

  // attackable positions assuming empty board
  def eyes(from: Pos, to: Pos): Boolean =
    role match {
      case King => from touches to

      case Rook => from onSameLine to

      case Bishop => from onSameDiagonal to

      case Gold | Tokin | PromotedSilver | PromotedKnight | PromotedLance if color == White =>
        (from touches to) && (to.y >= from.y || (to ?| from))
      case Gold | Tokin | PromotedSilver | PromotedKnight | PromotedLance if color == Black =>
        (from touches to) && (to.y <= from.y || (to ?| from))

      case Silver if color == White =>
        (from touches to) && from.y != to.y && (from.x != to.x || to.y > from.y)
      case Silver if color == Black =>
        (from touches to) && from.y != to.y && (from.x != to.x || to.y < from.y)

      case Knight if color == White =>
        from.color != to.color && {
          val xd = from xDist to
          val yd = from yDist to
          (xd == 1 && yd == 2 && from.y < to.y) // march only forward
        }
      case Knight if color == Black =>
        from.color != to.color && {
          val xd = from xDist to
          val yd = from yDist to
          (xd == 1 && yd == 2 && from.y > to.y) // march only forward
        }

      case Lance if color == White => (from ?| to) && (from ?+ to)
      case Lance if color == Black => (from ?| to) && (from ?^ to)

      case Pawn if color == White => from.y + 1 == to.y && from ?| to
      case Pawn if color == Black => from.y - 1 == to.y && from ?| to

      case Horse  => (from touches to) || (from onSameDiagonal to)
      case Dragon => (from touches to) || (from onSameLine to)
    }

  // movable positions assuming empty board
  def eyesMovable(from: Pos, to: Pos): Boolean =
    eyes(from, to)

  override def toString = (color + "-" + role).toLowerCase
}

object Piece {

  def fromChar(c: Char): Option[Piece] =
    Role.allByPgn get c.toUpper map {
      Piece(Color(c.isUpper), _)
    }

}
