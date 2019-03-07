package draughts
package variant

case object Breakthrough extends Variant(
  id = 9,
  gameType = 96,
  key = "breakthrough",
  name = "Breakthrough",
  shortName = "Breakthrough",
  title = "The first player who makes a king wins.",
  standardInitialPosition = true
) {

  def pieces = Standard.pieces

  // Win on promotion
  override def specialEnd(situation: Situation) =
    situation.board.kingPosOf(White).isDefined || situation.board.kingPosOf(Black).isDefined

  override def winner(situation: Situation): Option[Color] =
    if (situation.board.kingPosOf(White).isDefined) White.some
    else if (situation.board.kingPosOf(Black).isDefined) Black.some
    else None

  // No drawing rules
  override def updatePositionHashes(board: Board, move: Move, hash: draughts.PositionHash): PositionHash =
    Hash(Situation(board, !move.piece.color))

}
