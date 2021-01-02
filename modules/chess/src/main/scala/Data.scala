package chess

case class Data(
      pockets: Pockets,
      promoted: Set[Pos]
  ) {

    def drop(piece: Piece): Option[Data] =
      pockets take piece map { nps =>
        copy(pockets = nps)
      }

    def store(piece: Piece, from: Pos) =
      copy(
        pockets = pockets store Piece(piece.color, Role.demotesTo(piece.role)),
        promoted = promoted - from
      )

    def promote(pos: Pos) = copy(promoted = promoted + pos)

    def move(orig: Pos, dest: Pos) =
      copy(
        promoted = if (promoted(orig)) promoted - orig + dest else promoted
      )
  }

object Data {
    val init = Data(Pockets(Pocket(Nil), Pocket(Nil)), Set.empty)
    val storableRoles = List(Pawn, Knight, Silver, Gold, Bishop, Rook, Lance)
}

case class Pockets(white: Pocket, black: Pocket) {
  def apply(color: Color) = color.fold(white, black)
  def take(piece: Piece): Option[Pockets] =
    piece.color.fold(
      white take piece.role map { np =>
        copy(white = np)
      },
      black take piece.role map { np =>
        copy(black = np)
      }
    )
  def store(piece: Piece) =
    piece.color.fold(
      copy(black = black store piece.role),
      copy(white = white store piece.role)
    )
  def keys: String = white.roles.map(_.forsyth).mkString("").toUpperCase() + black.roles.map(_.forsyth).mkString("").toLowerCase()
}

case class Pocket(roles: List[Role]) {
  def take(role: Role) =
    if (roles contains role) Some(copy(roles = roles diff List(role)))
    else None
  def store(role: Role) =
    if (Data.storableRoles contains role) copy(roles = role :: roles)
    else this
}
