package chess

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

object Divider {

  def apply(boards: List[Board]): Division = {

    val indexedBoards: List[(Board, Int)] = boards.zipWithIndex

    val midGame = indexedBoards.foldLeft(none[Int]) {
      case (found: Some[_], _) => found
      case (_, (board, index)) =>
        (majorsAndMinors(board) <= 10 ||
          backrankSparse(board) ||
          mixedness(board) > 150) option index
    }

    val endGame =
      if (midGame.isDefined) indexedBoards.foldLeft(none[Int]) {
        case (found: Some[_], _) => found
        case (_, (board, index)) => (majorsAndMinors(board) <= 6) option index
      }
      else None

    Division(
      midGame.filter(m => endGame.fold(true)(m <)),
      endGame,
      boards.size
    )
  }

  private def majorsAndMinors(board: Board): Int =
    board.pieces.values.foldLeft(0) { (v, p) =>
      if (p.role == Pawn || p.role == King) v else v + 1
    }

  private val backranks = List(Pos.whiteBackrank -> Color.White, Pos.blackBackrank -> Color.Black)

  // Sparse back-rank indicates that pieces have been developed
  private def backrankSparse(board: Board): Boolean =
    backranks.exists {
      case (backrank, color) =>
        backrank.count { pos =>
          board(pos).fold(false)(_ is color)
        } < 4
    }

  private def score(white: Int, black: Int, y: Int): Int =
    (white, black) match {
      case (0, 0) => 0

      case (1, 0) => 1 + (8 - y)
      case (2, 0) => if (y > 2) 2 + (y - 2) else 0
      case (3, 0) => if (y > 1) 3 + (y - 1) else 0
      case (4, 0) => if (y > 1) 3 + (y - 1) else 0 // group of 4 on the homerow = 0

      case (0, 1) => 1 + y
      case (1, 1) => 5 + (3 - y).abs
      case (2, 1) => 4 + y
      case (3, 1) => 5 + y

      case (0, 2) => if (y < 6) 2 + (6 - y) else 0
      case (1, 2) => 4 + (6 - y)
      case (2, 2) => 7

      case (0, 3) => if (y < 7) 3 + (7 - y) else 0
      case (1, 3) => 5 + (6 - y)

      case (0, 4) => if (y < 7) 3 + (7 - y) else 0

      case _ => 0
    }

  private val mixednessRegions: List[List[Pos]] = {
    for {
      y <- 1 to 7
      x <- 1 to 7
    } yield {
      for {
        dy <- 0 to 1
        dx <- 0 to 1
      } yield Pos.posAt(x + dx, y + dy)
    }.toList.flatten
  }.toList

  private def mixedness(board: Board): Int = {
    val boardValues = board.pieces.view.mapValues(_ is Color.white)
    mixednessRegions.foldLeft(0) {
      case (mix, region) =>
        var white = 0
        var black = 0
        region foreach { p =>
          boardValues get p foreach { v =>
            if (v) white = white + 1
            else black = black + 1
          }
        }
        mix + score(white, black, region.head.y)
    }
  }
}
