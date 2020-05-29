package draughts
package variant

import scala.annotation.tailrec
import scala.collection.breakOut

case object Frisian extends Variant(
  id = 10,
  gameType = 40,
  key = "frisian",
  name = "Frisian",
  shortName = "Frisian",
  title = "Pieces can also capture horizontally and vertically.",
  standardInitialPosition = true,
  boardSize = Board.D100
) {

  def pieces = Standard.pieces
  def moveDirsColor = Standard.moveDirsColor
  def moveDirsAll = Standard.moveDirsAll

  val captureDirs: Directions = List((Variant.UpLeft, _.moveUpLeft), (Variant.UpRight, _.moveUpRight), (Variant.Up, _.moveUp), (Variant.DownLeft, _.moveDownLeft), (Variant.DownRight, _.moveDownRight), (Variant.Down, _.moveDown), (Variant.Left, _.moveLeft), (Variant.Right, _.moveRight))

  override def getCaptureValue(board: Board, taken: List[Pos]) = taken.foldLeft(0) {
    (t, p) => t + getCaptureValue(board, p)
  }
  override def getCaptureValue(board: Board, taken: Pos) =
    board(taken) match {
      case Some(piece) if piece.role == King => 199
      case Some(piece) if piece.role == Man => 100
      case _ => 0
    }

  override def validMoves(situation: Situation, finalSquare: Boolean = false): Map[Pos, List[Move]] = {

    var bestLineValue = 0
    var captureMap = Map[Pos, List[Move]]()
    var captureKing = false
    for (actor <- situation.actors) {
      val capts = if (finalSquare) actor.capturesFinal else actor.captures
      if (capts.nonEmpty) {
        val lineValue = capts.head.taken.fold(0)(getCaptureValue(situation.board, _))
        if (lineValue > bestLineValue) {
          bestLineValue = lineValue
          captureMap = Map(actor.pos -> capts)
          captureKing = actor.piece.role == King
        } else if (lineValue == bestLineValue) {
          if (!captureKing && (actor.piece is King)) {
            captureMap = Map(actor.pos -> capts)
            captureKing = true
          } else if (captureKing == (actor.piece is King))
            captureMap = captureMap + (actor.pos -> capts)
        }
      }
    }

    if (captureMap.nonEmpty)
      captureMap
    else
      situation.actors.collect {
        case actor if actor.noncaptures.nonEmpty =>
          actor.pos -> actor.noncaptures
      }(breakOut)
  }

  override def finalizeBoard(board: Board, uci: format.Uci.Move, captured: Option[List[Piece]], remainingCaptures: Int): Board = {
    if (remainingCaptures > 0) board
    else board.actorAt(uci.dest).fold(board) { act =>
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

  override def maxDrawingMoves(board: Board): Option[Int] =
    if (board.pieces.size <= 3 && board.roleCount(Man) == 0) {
      if (board.pieces.size == 3) Some(14)
      else Some(4)
    } else None

  /**
   * Update position hashes for frisian drawing rules:
   * - When one player has two kings and the other one, the game is drawn after both players made 7 moves.
   * - When bother players have one king left, the game is drawn after both players made 2 moves.  The official rules state that the game is drawn immediately when both players have only one king left, unless either player can capture the other king immediately or will necessarily be able to do this next move.  In absence of a good way to distinguish the positions that win by force from those that don't, this rule is implemented on lidraughts by always allowing 2 more moves to win the game.
   */
  override def updatePositionHashes(board: Board, move: Move, hash: draughts.PositionHash): PositionHash = {
    val newHash = Hash(Situation(board, !move.piece.color))
    maxDrawingMoves(board) match {
      case Some(drawingMoves) =>
        if (drawingMoves == 14 && move.captures)
          newHash // 7 move rule resets only when another piece disappears, activating the "2-move rule"
        else
          newHash ++ hash // 2 move rule never resets once activated
      case _ => newHash
    }
  }

}
