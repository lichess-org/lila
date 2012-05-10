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
    // last move and position hashes
    val h2 = h1.copy(
      positionHashes =
        if ((piece is Pawn) || captures || promotes || castles) Nil
        else h1 positionHashesWith after.positionHash,
      lastMove = Some(orig, dest)
    )
    // my broken castles
    val h3 =
      if ((piece is King) && h2.canCastle(color).any)
        h2 withoutCastles color
      else if (piece is Rook) (for {
        kingPos ← after kingPosOf color
        side ← Side.kingRookSide(kingPos, orig)
        if h2 canCastle color on side
      } yield h2.withoutCastle(color, side)) | h2
      else h2
    // opponent broken castles
    (for {
      cPos ← capture
      cPiece ← before(cPos)
      if cPiece is Rook
      kingPos ← after kingPosOf !color
      side ← Side.kingRookSide(kingPos, cPos)
      if h3 canCastle !color on side
    } yield h3.withoutCastle(!color, side)) | h3
  }

  // does this move capture an opponent piece?
  def captures = capture.isDefined

  def promotes = promotion.isDefined

  def castles = castle.isDefined

  def color = piece.color

  def notation = orig + " " + dest

  def withPromotion(op: Option[PromotableRole]): Option[Move] = op.fold(
    p ⇒ if ((after count color.queen) > (before count color.queen)) for {
      b2 ← after take dest
      b3 ← b2.place(color - p, dest)
    } yield copy(after = b3, promotion = Some(p))
    else None,
    Some(this)
  )

  override def toString = "%s %s".format(piece, notation)
}
