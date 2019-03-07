package draughts
package variant

case object Frysk extends Variant(
  id = 8,
  gameType = 97,
  key = "frysk",
  name = "Frysk!",
  shortName = "Frysk!",
  title = "Frisian draughts starting with 5 pieces each.",
  standardInitialPosition = false
) {

  def pieces = Variant.symmetricBackrank(standardRank)

  override def finalizeBoard(board: Board, uci: format.Uci.Move, captured: Option[List[Piece]], remainingCaptures: Int): Board = Frisian.finalizeBoard(board, uci, captured, remainingCaptures)
  override def updatePositionHashes(board: Board, move: Move, hash: draughts.PositionHash): PositionHash = Frisian.updatePositionHashes(board, move, hash)

}
