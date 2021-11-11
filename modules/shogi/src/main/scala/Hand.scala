package shogi

import variant.Variant

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

  def store(piece: Piece) =
    piece.color.fold(
      copy(gote = gote store piece.role),
      copy(sente = sente store piece.role)
    )

  def roles: List[Role] =
    (sente.roles ::: gote.roles).distinct

  def size: Int =
    sente.size + gote.size

  def valueOf(c: Color) =
    c.fold(sente.value, gote.value)

  def value: Int =
    sente.value - gote.value

  def impasseValueOf(c: Color) =
    c.fold(sente.impasseValue, gote.impasseValue)

}

object Hands {
  def apply(hand: Iterable[(Role, Int)]): Hands = Hands(Hand(hand), Hand(hand))
  def init(variant: Variant): Hands             = apply(variant.hand)
}

case class Hand(handMap: HandMap) {

  def apply(role: Role): Int =
    handMap.getOrElse(role, 0)

  def take(role: Role) =
    if (handMap.getOrElse(role, 0) > 0) Option(copy(handMap = handMap + (role -> (handMap(role) - 1))))
    else None

  def store(role: Role, cnt: Int = 1) =
    if (handMap contains role)
      copy(handMap = handMap + (role -> (handMap.getOrElse(role, 0) + cnt)))
    else this

  def roles: List[Role] =
    handMap.map(_._1).toList

  def size: Int =
    handMap.foldLeft(0)((acc, kv) => acc + kv._2)

  def value: Int =
    handMap.foldLeft(0) { (acc, kv) =>
      acc + Role.valueOf(kv._1) * kv._2
    }

  def impasseValue: Int =
    handMap.foldLeft(0) { (acc, kv) =>
      acc + Role.impasseValueOf(kv._1) * kv._2
    }

}

object Hand {
  def apply(hand: Iterable[(Role, Int)]): Hand = Hand(hand.toMap)
  def init(variant: Variant): Hand             = Hand(variant.hand)
}
