package draughts

import scala.annotation.tailrec

case class Actor(
    piece: Piece,
    pos: PosMotion,
    board: Board
) {

  lazy val validMoves: List[Move] = if (captures.nonEmpty) captures else noncaptures
  lazy val allMoves: List[Move] = captures ::: noncaptures
  lazy val noncaptures: List[Move] = noncaptureMoves()
  lazy val captures: List[Move] = captureMoves(false)

  lazy val allDestinations: List[Pos] = allMoves map (_.dest)

  /**
   * Same as captures, but giving the final destination square instead of the first.
   */
  lazy val capturesFinal: List[Move] = captureMoves(true)

  def captureLength = captures.foldLeft(0) {
    case (max, move) =>
      move.capture.fold(max) { jumps =>
        if (jumps.length > max) jumps.length
        else max
      }
  }

  private def noncaptureMoves(): List[Move] = piece.role match {
    case Man => shortRangeMoves(board.variant.moveDirsColor(color))
    case King =>
      if (board.variant.frisianVariant && board.history.kingMoves(color) >= 3 && board.history.kingMoves.kingPos(color).fold(true)(_ == pos)) Nil
      else longRangeMoves(board.variant.moveDirsAll)
    case _ => Nil
  }

  private def captureMoves(finalSquare: Boolean): List[Move] = piece.role match {
    case Man => board.variant.shortRangeCaptures(this, finalSquare)
    case King => board.variant.longRangeCaptures(this, finalSquare)
    case _ => Nil
  }

  def color: Color = piece.color
  def is(c: Color): Boolean = c == piece.color
  def is(p: Piece): Boolean = p == piece

  private def shortRangeMoves(dirs: Directions): List[Move] =
    dirs flatMap { _._2(pos) } flatMap { to =>
      board.pieces.get(to) match {
        case None => board.move(pos, to) map { move(to, _, None, None) } flatMap board.variant.maybePromote
        case Some(_) => Nil
      }
    }

  private def longRangeMoves(dirs: Directions): List[Move] = {
    val buf = new scala.collection.mutable.ArrayBuffer[Move]

    @tailrec
    def addAll(p: PosMotion, dir: Direction): Unit = {
      dir._2(p) match {
        case Some(to) =>
          board.pieces.get(to) match {
            case None =>
              board.move(pos, to).foreach { buf += move(to, _, None, None) }
              addAll(to, dir)
            case _ => // occupied
          }
        case _ => // past end of board
      }
    }

    dirs foreach { addAll(pos, _) }
    buf.toList
  }

  def move(
    dest: Pos,
    after: Board,
    /* Single capture or none */
    capture: Option[Pos],
    taken: Option[Pos]
  ) = Move(
    piece = piece,
    orig = pos,
    dest = dest,
    situationBefore = Situation(board, piece.color),
    after = after,
    capture = capture.map(List(_)),
    taken = taken.map(List(_))
  )

  def move(
    /** Destination square of the move */
    dest: Pos,
    /** Board after this move is made */
    after: Board,
    /** Chained captures (1x2x3) */
    capture: List[Pos],
    /** Pieces taken from the board */
    taken: List[Pos]
  ) = Move(
    piece = piece,
    orig = pos,
    dest = dest,
    situationBefore = Situation(board, piece.color),
    after = after,
    capture = capture.some,
    taken = taken.some
  )
}
