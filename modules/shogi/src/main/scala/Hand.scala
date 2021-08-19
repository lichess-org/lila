package shogi

case class Hands(sente: Hand, gote: Hand) {
  def apply(color: Color) = color.fold(sente, gote)

  def drop(piece: Piece): Option[Hands] =
    piece.color.fold(
      sente take piece.role map { nh =>
        copy(sente = nh)
      },
      gote take piece.role map { nh =>
        copy(gote = nh)
      }
    )

  def store(piece: Piece) = {
    val unpromotedRole = Role.demotesTo(piece.role).getOrElse(piece.role)
    piece.color.fold(
      copy(gote = gote store unpromotedRole),
      copy(sente = sente store unpromotedRole)
    )
  }

  def size: Int =
    sente.size + gote.size

  def valueOf(c: Color) =
    c.fold(sente.value, gote.value)

  def value: Int =
    sente.value - gote.value

  def impasseValueOf(c: Color) =
    c.fold(sente.impasseValue, gote.impasseValue)

  def exportHands: String = {
    val pocketStr = sente.exportHand.toUpperCase() + gote.exportHand.toLowerCase()
    if (pocketStr == "") "-"
    else pocketStr
  }

  override def toString = s"$exportHands"
}

object Hands {
  val init = Hands(Hand.init, Hand.init)
}

case class Hand(roleMap: HandMap) {

  def apply(role: Role): Int =
    roleMap.getOrElse(role, 0)

  def take(role: Role) =
    if (roleMap.getOrElse(role, 0) > 0) Some(copy(roleMap = roleMap + (role -> (roleMap(role) - 1))))
    else None

  def store(role: Role, cnt: Int = 1) =
    if (Role.handRoles contains role) copy(roleMap = roleMap + (role -> (roleMap.getOrElse(role, 0) + cnt)))
    else this

  def exportHand: String =
    Role.handRoles.map { r =>
      val cnt = roleMap.getOrElse(r, 0)
      if (cnt == 1) r.forsythFull
      else if (cnt > 1) cnt.toString + r.forsythFull
      else ""
    } mkString ""

  def size: Int =
    roleMap.foldLeft(0)((acc, kv) => acc + kv._2)

  def value: Int =
    roleMap.foldLeft(0) { (acc, kv) =>
      acc + Role.valueOf(kv._1) * kv._2
    }

  def impasseValue: Int =
    roleMap.foldLeft(0) { (acc, kv) =>
      acc + Role.impasseValueOf(kv._1) * kv._2
    }

}

object Hand {
  val initMap: HandMap =
    Map(Rook -> 0, Bishop -> 0, Gold -> 0, Silver -> 0, Knight -> 0, Lance -> 0, Pawn -> 0)
  val init = Hand(initMap)
}
