package shogi

case class Piece(color: Color, role: Role) {

  def is(c: Color)   = c == color
  def is(r: Role)    = r == role
  def isNot(r: Role) = r != role

  def oneOf(rs: Set[Role]) = rs(role)

  // for board representation
  def forsyth: String = if (color.sente) role.forsythUpper else role.forsyth
  def csa: String     = if (color.sente) s"+${role.csa}" else s"-${role.csa}"
  def kif: String     = if (color.sente) s" ${role.kifSingle}" else s"v${role.kifSingle}"

  def directDirs = if (color.sente) role.senteDirectDirs else role.goteDirectDirs
  def projectionDirs  = if (color.sente) role.senteProjectionDirs else role.goteProjectionDirs

  def updateRole(f: Role => Option[Role]): Option[Piece] =
    f(role).map(r => copy(role = r))

  def switch = copy(color = !color)

  // attackable positions assuming empty full-sized board
  def eyes(from: Pos, to: Pos): Boolean =
    role match {
      case King => from touches to

      case Rook => from onSameLine to

      case Bishop => from onSameDiagonal to

      case Gold | Tokin | PromotedSilver | PromotedKnight | PromotedLance if color.sente =>
        (from touches to) && (to.rank <= from.rank || (to ?| from))
      case Gold | Tokin | PromotedSilver | PromotedKnight | PromotedLance =>
        (from touches to) && (to.rank >= from.rank || (to ?| from))

      case Silver if color.sente =>
        (from touches to) && from.rank != to.rank && (from.file != to.file || to.rank < from.rank)
      case Silver =>
        (from touches to) && from.rank != to.rank && (from.file != to.file || to.rank > from.rank)

      case Knight if color.sente =>
        val xd = from xDist to
        val yd = from yDist to
        (xd == 1 && yd == 2 && from.rank > to.rank)
      case Knight =>
        val xd = from xDist to
        val yd = from yDist to
        (xd == 1 && yd == 2 && from.rank < to.rank)

      case Lance if color.sente => (from ?| to) && (from ?+ to)
      case Lance                => (from ?| to) && (from ?^ to)

      case Pawn if color.sente => from.rank.index - 1 == to.rank.index && from ?| to
      case Pawn                => from.rank.index + 1 == to.rank.index && from ?| to

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
