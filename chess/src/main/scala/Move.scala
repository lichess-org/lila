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

  def finalizeAfter: Board = after updateHistory { h1 ⇒
    h1.copy(
      positionHashes =
        if ((piece is Pawn) || captures || promotes || castles) Nil
        else h1 positionHashesWith after.positionHash,
      lastMove = Some(orig, dest)
    ) |> { h2 ⇒
        if ((piece is King) && h2.canCastle(color).any)
          h2 withoutCastles color
        else if (piece is Rook) (for {
          kingPos ← after kingPosOf color
          side ← Side.kingRookSide(kingPos, orig)
          if h2 canCastle color on side
        } yield h2.withoutCastle(color, side)) | h2
        else h2
      }
  }

  // does this move capture an opponent piece?
  def captures = capture.isDefined

  def promotes = promotion.isDefined

  def castles = castle.isDefined

  def color = piece.color

  override def toString = orig + " " + dest
}
