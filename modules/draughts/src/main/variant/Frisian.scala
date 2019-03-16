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
      board.actorAt(uci.dest).fold(board) { act =>
        val tookLastMan = captured.fold(false)(_.exists(_.role == Man)) && board.count(Man, !act.color) == 0
        val remainingMen = board.count(Man, act.color)
        if (remainingMen != 0)
          board updateHistory { h =>
            val kingmove = act.piece.role == King && uci.promotion.isEmpty && captured.fold(true)(_.isEmpty)
            val differentKing = kingmove && act.color.fold(h.kingMoves.whiteKing, h.kingMoves.blackKing).fold(false)(_ != uci.orig)
            val hist = if (differentKing) h.withKingMove(act.color, none, false) else h
            hist.withKingMove(act.color, uci.dest.some, kingmove, tookLastMan)
          }
        else {
          val promotedLastMan = uci.promotion.nonEmpty
          if (tookLastMan)
            board updateHistory { h =>
              val hist = if (promotedLastMan) h.withKingMove(act.color, none, false) else h
              h.withKingMove(!act.color, none, false)
            }
          else if (promotedLastMan)
            board updateHistory { _.withKingMove(act.color, none, false) }
          else
            board
        }
      } withoutGhosts
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
