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
  override def initialFen = "W:W46,47,48,49,50:B1,2,3,4,5:H0:F1"

  override val captureDirs: Directions = Frisian.captureDirs

  @inline
  override def captureValue(board: Board, taken: List[Pos]) = Frisian.captureValue(board, taken)
  @inline
  override def captureValue(board: Board, taken: Pos) = Frisian.captureValue(board, taken)

  override def validMoves(situation: Situation, finalSquare: Boolean = false): Map[Pos, List[Move]] = Frisian.validMoves(situation, finalSquare)
  override def finalizeBoard(board: Board, uci: format.Uci.Move, captured: Option[List[Piece]], remainingCaptures: Int): Board = Frisian.finalizeBoard(board, uci, captured, remainingCaptures)
  override def updatePositionHashes(board: Board, move: Move, hash: draughts.PositionHash): PositionHash = Frisian.updatePositionHashes(board, move, hash)

  override protected def validSide(board: Board, strict: Boolean)(color: Color) = {
    val roles = board rolesOf color
    (roles.count(_ == Man) > 0 || roles.count(_ == King) > 0) &&
      (!strict || roles.size <= 5) &&
      (!menOnPromotionRank(board, color) || board.ghosts != 0)
  }

}
