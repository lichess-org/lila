package lila.chess

case class Move(
    piece: Piece,
    orig: Pos,
    dest: Pos,
    before: Board,
    after: Board,
    capture: Option[Pos],
    promotion: Option[PromotableRole],
    castles: Boolean,
    enpassant: Boolean) {

  def withHistory(h: History) = copy(after = after withHistory h)

  def afterWithPositionHashesUpdated: Board = after updateHistory { h ⇒
    if ((piece is Pawn) || captures || promotes || castles) h.withoutPositionHashes
    else h withNewPositionHash after.positionHash
  }

  // does this move capture an opponent piece?
  def captures = capture.isDefined

  def promotes = promotion.isDefined

  def color = piece.color
}
