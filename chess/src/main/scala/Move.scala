package lila.chess

case class Move(
    piece: Piece,
    orig: Pos,
    dest: Pos,
    before: Board,
    after: Board,
    capture: Option[Pos],
    promotion: Option[PromotableRole],
    castle: Option[((Pos, Pos), (Pos, Pos))],
    enpassant: Boolean) {

  def withHistory(h: History) = copy(after = after withHistory h)

  def finalizeAfter: Board = after updateHistory { h ⇒
    h.copy(
      positionHashes =
        if ((piece is Pawn) || captures || promotes || castles) Nil
        else h positionHashesWith after.positionHash,
      lastMove = Some(orig, dest)
    )
  }

  // does this move capture an opponent piece?
  def captures = capture.isDefined

  def promotes = promotion.isDefined

  def castles = castle.isDefined

  def color = piece.color

  override def toString = orig + " " + dest
}
