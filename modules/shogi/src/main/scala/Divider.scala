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
final case class NotFound(senteInvadersInGotesCamp: Int = 0, goteInvadersInSentesCamp: Int = 0)
    extends DividerData
final case class Found(index: Int) extends DividerData

object Divider {

  def apply(boards: List[Board]): Division = {

    val indexedBoards: List[(Board, Int)] = boards.zipWithIndex

    val midGame = indexedBoards.foldLeft(NotFound(): DividerData) {
      case (found: Found, _) => found
      case (NotFound(lastSenteInvaders, lastGoteInvaders), (board, index)) => {
        val curSenteInvaders = countInvaders(board, Sente)
        val curGoteInvaders  = countInvaders(board, Gote)
        if (
          // every piece except pawns and king
          majorsAndMinors(board) <= 17 ||
          isBackrankSparse(board) ||
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
      case Found(index) => Option(index)
      case _            => None
    }

    val endGame =
      if (midGameOption.isDefined) indexedBoards.foldLeft(NotFound(): DividerData) {
        case (found: Found, _) => found
        case (NotFound(lastSenteInvaders, lastGoteInvaders), (board, index)) => {
          val curSenteInvaders = countInvaders(board, Sente)
          val curGoteInvaders  = countInvaders(board, Gote)
          if (
            (majorsAndMinors(board) <= 3 && board.pieces.size <= 12) ||
            (curSenteInvaders >= 2 && lastSenteInvaders >= 2) ||
            (curGoteInvaders >= 2 && lastGoteInvaders >= 2)
          ) Found(index)
          else
            NotFound(curSenteInvaders, curGoteInvaders)
        }
      }
      else NotFound()

    val endGameOption = endGame match {
      case Found(index) => Option(index)
      case _            => None
    }

    Division(
      midGameOption.filter(m => endGameOption.fold(true)(m <)),
      endGameOption,
      boards.size
    )
  }

  private def countInvaders(board: Board, color: Color): Int =
    board.pieces.count { case (pos, piece) =>
      piece.color == color && board.variant.promotionRanks(color).contains(pos.y)
    }

  private def majorsAndMinors(board: Board): Int =
    board.pieces.values.count(p => p.role != Pawn && p.role != King)

  // Sparse back-rank indicates that pieces have been developed
  private def isBackrankSparse(board: Board): Boolean =
    board.pieces.count { case (pos, piece) =>
      board.variant.backrank(!piece.color) == pos.y
    } < 4

}
