package shogi

case class Piece(color: Color, role: Role) {

  def is(c: Color)   = c == color
  def is(r: Role)    = r == role
  def isNot(r: Role) = r != role

  def oneOf(rs: Set[Role]) = rs(role)

  // for board representation
  def forsyth: String = if (color == Sente) role.forsythUpper else role.forsyth
  def csa: String     = if (color == Sente) s"+${role.csa}" else s"-${role.csa}"
  def kif: String     = if (color == Sente) s" ${role.kifSingle}" else s"v${role.kifSingle}"

  def shortRangeDirs = if (color == Sente) role.senteShortDirs else role.goteShortDirs
  def longRangeDirs  = if (color == Sente) role.senteProjectionDirs else role.goteProjectionDirs

  def updateRole(f: Role => Option[Role]): Option[Piece] =
    f(role) map { r => copy(role = r) }

  // attackable positions assuming empty full-sized (9x9) board
  def eyes(from: Pos, to: Pos): Boolean =
    role match {
      case King => from touches to

      case Rook => from onSameLine to

      case Bishop => from onSameDiagonal to

      case Gold | Tokin | PromotedSilver | PromotedKnight | PromotedLance if color == Sente =>
        (from touches to) && (to.y <= from.y || (to ?| from))
      case Gold | Tokin | PromotedSilver | PromotedKnight | PromotedLance =>
        (from touches to) && (to.y >= from.y || (to ?| from))

      case Silver if color == Sente =>
        (from touches to) && from.y != to.y && (from.x != to.x || to.y < from.y)
      case Silver =>
        (from touches to) && from.y != to.y && (from.x != to.x || to.y > from.y)

      case Knight if color == Sente =>
        val xd = from xDist to
        val yd = from yDist to
        (xd == 1 && yd == 2 && from.y > to.y)
      case Knight =>
        val xd = from xDist to
        val yd = from yDist to
        (xd == 1 && yd == 2 && from.y < to.y)

      case Lance if color == Sente => (from ?| to) && (from ?+ to)
      case Lance                   => (from ?| to) && (from ?^ to)

      case Pawn if color == Sente => from.y - 1 == to.y && from ?| to
      case Pawn                   => from.y + 1 == to.y && from ?| to

      case Horse  => (from touches to) || (from onSameDiagonal to)
      case Dragon => (from touches to) || (from onSameLine to)
    }

  override def toString = s"$color-$role".toLowerCase
}

object Piece {

  def fromForsyth(s: String): Option[Piece] =
    Role.allByForsyth get s.toLowerCase map { r =>
      Piece(Color.fromSente(s != r.forsyth), r)
    }

  def fromCsa(s: String): Option[Piece] =
    Role.allByCsa get s.drop(1) map {
      Piece(Color.fromSente(s.take(1) == "+"), _)
    }

}
