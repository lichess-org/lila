package shogi

case class Division(middle: Option[Int], end: Option[Int], plies: Int) {

  def openingSize: Int = middle | plies
  def middleSize: Option[Int] =
    middle.map { m =>
      (end | plies) - m
    }
  def endSize = end.map(plies -)

  def openingBounds = middle.map(0 -> _)
  def middleBounds =
    for {
      m <- middle
      e <- end
    } yield m -> e
  def endBounds = end.map(_ -> plies)
}

object Division {
  val empty = Division(None, None, 0)
}

sealed trait DividerData
final case class NotFound(senteInvadersInGotesCamp: Int, goteInvadersInSentesCamp: Int) extends DividerData
final case class Found(index: Int)                                                      extends DividerData

object Divider {

  def apply(sits: Seq[Situation]): Division = {

    val indexedSits = sits.zipWithIndex

    val midGame = indexedSits.foldLeft[DividerData](NotFound(0, 0)) {
      case (found: Found, _) => found
      case (NotFound(lastSenteInvaders, lastGoteInvaders), (sit, index)) => {
        val curSenteInvaders = countInvaders(sit, Sente)
        val curGoteInvaders  = countInvaders(sit, Gote)
        if (
          // every piece except pawns and king
          majorsAndMinors(sit) <= 17 ||
          isBackrankSparse(sit) ||
          // if one piece invades the opposing camp and is not immediately captured
          (curSenteInvaders >= 1 && lastSenteInvaders >= 1) ||
          (curGoteInvaders >= 1 && lastGoteInvaders >= 1)
        ) Found(index)
        else
          // store the current # of Invaders
          NotFound(curSenteInvaders, curGoteInvaders)
      }
    }

    val midGameOption = midGame match {
      case Found(index) => Some(index)
      case _            => None
    }

    val endGame =
      if (midGameOption.isDefined) indexedSits.foldLeft[DividerData](NotFound(0, 0)) {
        case (found: Found, _) => found
        case (NotFound(lastSenteInvaders, lastGoteInvaders), (sit, index)) => {
          val curSenteInvaders = countInvaders(sit, Sente)
          val curGoteInvaders  = countInvaders(sit, Gote)
          if (
            (majorsAndMinors(sit) <= 3 && sit.board.pieces.size <= 12) ||
            (curSenteInvaders >= 2 && lastSenteInvaders >= 2) ||
            (curGoteInvaders >= 2 && lastGoteInvaders >= 2)
          ) Found(index)
          else
            NotFound(curSenteInvaders, curGoteInvaders)
        }
      }
      else NotFound(0, 0)

    val endGameOption = endGame match {
      case Found(index) => Some(index)
      case _            => None
    }

    Division(
      midGameOption.filter(m => endGameOption.fold(true)(m <)),
      endGameOption,
      sits.size
    )
  }

  private def countInvaders(sit: Situation, color: Color): Int =
    sit.board.pieces.count { case (pos, piece) =>
      piece.color == color && sit.variant.promotionRanks(color).contains(pos.rank)
    }

  private def majorsAndMinors(sit: Situation): Int =
    sit.board.pieces.values.count(p => p.role != Pawn && p.role != King)

  // Sparse back-rank indicates that pieces have been developed
  private def isBackrankSparse(sit: Situation): Boolean =
    sit.board.pieces.count { case (pos, piece) =>
      sit.variant.backrank(!piece.color) == pos.rank
    } < 4

}
