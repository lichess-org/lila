package shogi

import format.Usi

case class Move(
    piece: Piece,
    orig: Pos,
    dest: Pos,
    situationBefore: Situation,
    after: Board,
    capture: Option[Pos],
    promotion: Boolean,
    metrics: MoveMetrics = MoveMetrics()
) {
  def before = situationBefore.board

  def situationAfter = Situation(finalizeAfter, !piece.color)

  def withHistory(h: History) = copy(after = after withHistory h)

  def finalizeAfter: Board = {
    val board = after.variant.finalizeBoard(
      after updateHistory { h =>
        h.copy(
          lastMove = Option(toUsi)
        )
      },
      toUsi,
      capture flatMap before.apply,
      !situationBefore.color
    )

    // Update position hashes last, only after updating the board
    // todo isIrreversible for chushogi
    board updateHistory { h =>
      lazy val basePositionHashes =
        if (h.positionHashes.isEmpty) Hash(situationBefore) else h.positionHashes
      h.copy(positionHashes = Hash(Situation(board, !piece.color)) ++ basePositionHashes)
    }
  }

  def applyVariantEffect: Move = before.variant addVariantEffect this

  // does this move capture an opponent piece?
  def captures = capture.isDefined

  def promotes = promotion

  def color = piece.color

  def withAfter(newBoard: Board) = copy(after = newBoard)

  def withMetrics(m: MoveMetrics) = copy(metrics = m)

  def toUsi = {
    Usi.Move(orig, dest, promotion)
  }

  override def toString = s"$piece ${toUsi.usi}"
}
