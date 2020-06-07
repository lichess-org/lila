package draughts
package variant

case object Frysk extends Variant(
  id = 8,
  gameType = 97,
  key = "frysk",
  name = "Frysk!",
  shortName = "Frysk",
  title = "Frisian draughts starting with 5 pieces each.",
  standardInitialPosition = false,
  boardSize = Board.D100
) {

  val pieces = Variant.symmetricBackrank(Vector(Man, Man, Man, Man, Man), boardSize)
  val initialFen = "W:W46,47,48,49,50:B1,2,3,4,5:H0:F1"
  val startingPosition = StartingPosition("---", initialFen, "", "Initial position".some)

  def captureDirs = Frisian.captureDirs
  def moveDirsColor = Frisian.moveDirsColor
  def moveDirsAll = Frisian.moveDirsAll

  override def getCaptureValue(board: Board, taken: List[Pos]) = Frisian.getCaptureValue(board, taken)
  override def getCaptureValue(board: Board, taken: Pos) = Frisian.getCaptureValue(board, taken)

  override def validMoves(situation: Situation, finalSquare: Boolean = false): Map[Pos, List[Move]] = Frisian.validMoves(situation, finalSquare)
  override def finalizeBoard(board: Board, uci: format.Uci.Move, captured: Option[List[Piece]], situationBefore: Situation, finalSquare: Boolean): Board = Frisian.finalizeBoard(board, uci, captured, situationBefore, finalSquare)

  override def maxDrawingMoves(board: Board): Option[Int] = Frisian.maxDrawingMoves(board)
  override def updatePositionHashes(board: Board, move: Move, hash: draughts.PositionHash): PositionHash = Frisian.updatePositionHashes(board, move, hash)

  override protected def validSide(board: Board, strict: Boolean)(color: Color) = {
    val roles = board rolesOf color
    (roles.count(_ == Man) > 0 || roles.count(_ == King) > 0) &&
      (!strict || roles.size <= 5) &&
      (!menOnPromotionRank(board, color) || board.ghosts != 0)
  }

}
