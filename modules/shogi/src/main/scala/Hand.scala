package shogi

case class Hands(sente: Hand, gote: Hand) {
  def apply(color: Color) = color.fold(sente, gote)

  def take(piece: Piece, cnt: Int = 1): Option[Hands] =
    piece.color.fold(
      sente.take(piece.role, cnt) map { nh =>
        copy(sente = nh)
      },
      gote.take(piece.role, cnt) map { nh =>
        copy(gote = nh)
      }
    )

  def store(piece: Piece, cnt: Int = 1) =
    piece.color.fold(
      copy(sente = sente.store(piece.role, cnt)),
      copy(gote = gote.store(piece.role, cnt))
    )

  def pieces: List[Piece] =
    sente.roles.map(Piece(Sente, _)) ::: gote.roles.map(Piece(Gote, _))

  def piecesOf(color: Color): List[Piece] =
    color.fold(
      sente.roles.map(Piece(color, _)),
      gote.roles.map(Piece(color, _))
    )

  def roles: List[Role] =
    (sente.roles ::: gote.roles).distinct

  def size: Int =
    sente.size + gote.size

  def isEmpty: Boolean =
    sente.isEmpty && gote.isEmpty

  def nonEmpty: Boolean =
    !isEmpty

  def roleValue: Int =
    sente.roleValue - gote.roleValue

  def impasseValueOf(c: Color) =
    c.fold(sente.impasseValue, gote.impasseValue)

}

object Hands {
  def apply(sente: Iterable[(Role, Int)], gote: Iterable[(Role, Int)]): Hands = 
    new Hands(Hand(sente), Hand(gote))

  def apply(variant: shogi.variant.Variant): Hands = {
    val (s, g) = variant.hands.partitionMap {
      case (piece, cnt) if piece.color.sente => Left(piece.role -> cnt)
      case (piece, cnt) => Right(piece.role -> cnt)
    }
    Hands(s, g)
  }
  
  def empty: Hands = Hands(Nil, Nil)
}

case class Hand(handMap: HandMap) extends AnyVal {

  def apply(role: Role): Int =
    handMap.getOrElse(role, 0)

  def take(role: Role, cnt: Int = 1) =
    handMap.get(role).filter(_ - cnt >= 0).map(cur => copy(handMap = handMap + (role -> (cur - cnt))))

  def store(role: Role, cnt: Int = 1) =
    copy(handMap = handMap + (role -> (apply(role) + cnt)))

  def roles: List[Role] =
    handMap.view.filter(_._2 > 0).map(_._1).toList

  def size: Int =
    handMap.values.sum

  def nonEmpty: Boolean =
    handMap.exists(_._2 > 0)

  def isEmpty: Boolean =
    !nonEmpty

  def roleValue: Int =
    handMap.foldLeft(0) { case (acc, (role, cnt)) =>
      acc + Role.valueOf(role) * cnt
    }

  def impasseValue: Int =
    handMap.foldLeft(0) { case (acc, (role, cnt)) =>
      acc + Role.impasseValueOf(role) * cnt
    }

}

object Hand {

  def apply(hand: Iterable[(Role, Int)]): Hand = 
    new Hand(hand.toMap)
  
  def empty: Hand = Hand(Nil)

}
