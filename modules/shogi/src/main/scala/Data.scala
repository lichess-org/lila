package shogi

case class Data(
    pockets: Pockets
) {

  def drop(piece: Piece): Option[Data] =
    pockets take piece map { nps =>
      copy(pockets = nps)
    }

  def store(piece: Piece, from: Pos) =
    copy(
      pockets = pockets store Piece(piece.color, Role.demotesTo(piece.role).getOrElse(piece.role))
    )
}

object Data {
  val init = Data(Pockets(Pocket(Nil), Pocket(Nil)))
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
  
  def keys: String = sente.roles.map(_.forsyth).mkString("").toUpperCase() + gote.roles
    .map(_.forsyth)
    .mkString("")
    .toLowerCase()
  
  def exportPockets: String = {
    val pocketStr = sente.exportPocket.toUpperCase() + gote.exportPocket.toLowerCase()
    if (pocketStr == "") "-"
    else pocketStr
  }

  def size: Int =
    sente.size + gote.size

  def size(color: Color): Int =
    color.fold(sente.size, gote.size)

  def valueOf: Int =
    sente.valueOf - gote.valueOf
}

case class Pocket(roles: List[Role]) {
  def take(role: Role) =
    if (roles contains role) Some(copy(roles = roles diff List(role)))
    else None
  
  def store(role: Role) =
    if (Data.storableRoles contains role) copy(roles = role :: roles)
    else this

  def size: Int = roles.size
  
  def exportPocket: String =
    Data.storableRoles.map { r =>
      val cnt = roles.count(_ == r)
      if (cnt == 1) r.forsythFull
      else if (cnt > 1) cnt.toString + r.forsythFull
      else ""
    } mkString ""

  def valueOf: Int =
    roles.foldLeft(0) { (acc, curRole) =>
      Role.valueOf(curRole).fold(acc) { value =>
        acc + value
      }
    }
}
