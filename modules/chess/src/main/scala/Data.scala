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
      )

    def promote(pos: Pos) = copy(promoted = promoted + pos)

    def move(orig: Pos, dest: Pos) =
      copy(
        promoted = if (promoted(orig)) promoted - orig + dest else promoted
      )
  }

object Data {
    val init = Data(Pockets(Pocket(Nil), Pocket(Nil)), Set.empty)
    // correct order
    val storableRoles = List(Rook, Bishop, Gold, Silver, Knight, Lance, Pawn)
}

case class Pockets(sente: Pocket, gote: Pocket) {
  def apply(color: Color) = color.fold(sente, gote)
  def take(piece: Piece): Option[Pockets] =
    piece.color.fold(
      sente take piece.role map { np =>
        copy(sente = np)
      },
      gote take piece.role map { np =>
        copy(gote = np)
      }
    )
  def store(piece: Piece) =
    piece.color.fold(
      copy(gote = gote store piece.role),
      copy(sente = sente store piece.role)
    )
  def keys: String = sente.roles.map(_.forsyth).mkString("").toUpperCase() + gote.roles.map(_.forsyth).mkString("").toLowerCase()
  def exportPockets: String = {
    val pocketStr = sente.exportPocket.toUpperCase() + gote.exportPocket.toLowerCase()
    if(pocketStr == "") "-"
    else pocketStr
  }
}

case class Pocket(roles: List[Role]) {
  def take(role: Role) =
    if (roles contains role) Some(copy(roles = roles diff List(role)))
    else None
  def store(role: Role) =
    if (Data.storableRoles contains role) copy(roles = role :: roles)
    else this
  def exportPocket: String =
    Data.storableRoles.map{ r =>
      val cnt = roles.count(_ == r)
      if (cnt == 1) r.forsythFull
      else if (cnt > 1) cnt.toString + r.forsythFull
      else ""
    } mkString ""
}
