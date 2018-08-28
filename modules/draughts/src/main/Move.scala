package draughts

import format.Uci

case class Move(
    piece: Piece,
    orig: Pos,
    dest: Pos,
    situationBefore: Situation,
    after: Board,
    capture: Option[List[Pos]],
    taken: Option[List[Pos]],
    promotion: Option[PromotableRole],
    metrics: MoveMetrics = MoveMetrics()
) {

  def before = situationBefore.board

  def situationAfter = {
    val newBoard = finalizeAfter
    if (newBoard.ghosts != 0)
      Situation(newBoard, piece.color)
    else
      Situation(newBoard, !piece.color)
  }

  def withHistory(h: DraughtsHistory) = copy(after = after withHistory h)

  def finalizeAfter: Board = {
    val board = after updateHistory { h1 =>
      h1.copy(lastMove = Some(toUci))
    }

    board.variant.finalizeBoard(board, toUci, taken flatMap before.apply, situationBefore.captureLengthFrom(orig).getOrElse(0) - 1) updateHistory { h =>
      // Update position hashes last, only after updating the board,
      h.copy(positionHashes = board.variant.updatePositionHashes(board, this, h.positionHashes))
    }
  }

  def applyVariantEffect: Move = before.variant addVariantEffect this

  def afterWithLastMove = after.variant.finalizeBoard(
    after.copy(history = after.history.withLastMove(toUci)),
    toUci,
    taken flatMap before.apply,
    situationBefore.captureLengthFrom(orig).getOrElse(0) - 1
  )

  // does this move capture an opponent piece?
  def captures = capture.fold(false)(_.nonEmpty)

  def promotes = promotion.isDefined

  def color = piece.color

  def withPromotion(op: Option[PromotableRole]): Option[Move] =
    op.fold(this.some) { p =>
      if ((after count color.king) > (before count color.king)) for {
        b2 ← after take dest
        b3 ← b2.place(color - p, dest)
      } yield copy(after = b3, promotion = Some(p))
      else this.some
    }

  def withAfter(newBoard: Board) = copy(after = newBoard)

  def withMetrics(m: MoveMetrics) = copy(metrics = m)

  def toUci = Uci.Move(orig, dest, promotion, capture)
  def toShortUci = Uci.Move(orig, dest, promotion, if (capture.isDefined) capture.get.takeRight(1).some else None)

  override def toString = s"$piece ${toUci.uci}"

  def frisianValue = taken.fold(0f)(takes => {
    var sum = 0f
    var i = 0
    while (i < takes.length) {
      before(takes(i)) match {
        case Some(p) if p.role == King => sum += 1.99f
        case Some(p) if p.role == Man => sum += 1.0f
        case _ =>
      }
      i += 1
    }
    sum
  })

}
