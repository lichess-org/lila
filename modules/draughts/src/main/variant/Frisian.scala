package draughts
package variant

case object Frisian extends Variant(
  id = 10,
  gameType = 40,
  key = "frisian",
  name = "Frisian",
  shortName = "Frisian",
  title = "Capturing is also allowed horizontally and vertically.",
  standardInitialPosition = true
) {

  def pieces = Standard.pieces

  override def finalizeBoard(board: Board, uci: format.Uci.Move, captured: Option[List[Piece]], remainingCaptures: Int): Board = {
    if (remainingCaptures > 0)
      board
    else {
      val tookMan = captured.fold(false)(_.exists(_.role == Man))
      (board.actorAt(uci.dest) match {
        case Some(act) if board.count(Man, act.color) != 0 => board updateHistory { _.withKingMove(act.color, act.piece.role == King && uci.promotion.isEmpty && captured.fold(true)(_.isEmpty), tookMan && board.count(Man, !act.color) == 0) }
        case Some(act) if tookMan && board.count(Man, !act.color) == 0 => board updateHistory { _.withKingMove(!act.color, false) }
        case _ => board
      }).withouthGhosts()
    }
  }

  /**
   * Update position hashes for frisian drawing rules:
   * - When one player has two kings and the other one, the game is drawn after both players made 7 moves.
   * - When bother players have one king left, the game is drawn after both players made 2 moves.  The official rules state that the game is drawn immediately when both players have only one king left, unless either player can capture the other king immediately or will necessarily be able to do this next move.  In absence of a good way to distinguish the positions that win by force from those that don't, this rule is implemented on lidraughts by always allowing 2 more moves to win the game.
   */
  override def updatePositionHashes(board: Board, move: Move, hash: draughts.PositionHash): PositionHash = {
    val newHash = Hash(Situation(board, !move.piece.color))
    maxDrawingMoves(board) match {
      case Some(drawingMoves) =>
        if (drawingMoves == 14 && move.captures) newHash //7 move rule resets only when another piece disappears, activating the "2-move rule"
        else newHash ++ hash //2 move rule never resets once activated
      case _ => newHash
    }
  }

}
