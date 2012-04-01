package lila.chess

import Pos.posAt

// does not support chess960 castles!
final class ReverseEngineering(fromGame: Game, to: Board) {

  val from = fromGame.board

  def move: Valid[(Pos, Pos)] = {
    if (from.pieces == to.pieces) !!("from and to are similar") else findMove
  }

  private def findMove: Valid[(Pos, Pos)] =
    findMovedPieces match {
      case List((pos, piece)) ⇒ findPieceNewPos(pos, piece) map { np ⇒ (pos, np) }
      case List((pos1, piece1), (pos2, piece2)) if (piece1 is King) && (piece2 is Rook) ⇒
        findCastle(pos2) map { np ⇒ (pos1, np) }
      case List((pos1, piece1), (pos2, piece2)) if (piece1 is Rook) && (piece2 is King) ⇒
        findCastle(pos1) map { np ⇒ (pos2, np) }
      case l @ List((pos1, piece1), (pos2, piece2)) ⇒ !!("two moved pieces, but not a castle " + l)
      case l                                        ⇒ !!("no moved piece found " + l)
    }

  private def findCastle(rookPos: Pos): Valid[Pos] = rookPos.x match {
    case 1 ⇒ posAt(3, rookPos.y) toSuccess "invalid rook pos".wrapNel
    case 8 ⇒ posAt(7, rookPos.y) toSuccess "invalid rook pos".wrapNel
    case _ ⇒ !!("fail to find castle, rook on " + rookPos)
  }

  private def findPieceNewPos(pos: Pos, piece: Piece): Valid[Pos] = (for {
    dests ← fromGame.situation.destinations get pos
    dest ← dests find { to(_) map (_ is piece.color) getOrElse false }
  } yield dest) toSuccess "can not find piece new pos".wrapNel

  private def findMovedPieces: List[(Pos, Piece)] = {

    val fromPlayerPieces = from piecesOf fromGame.player
    val toPlayerPieces = to piecesOf fromGame.player

    fromPlayerPieces map {
      case (pos, piece) ⇒ (pos, piece, toPlayerPieces get pos)
    } collect {
      case (pos, piece, None)                                ⇒ pos -> piece
      case (pos, piece, Some(newPiece)) if piece != newPiece ⇒ pos -> piece
    } toList
  }

  private def !!(msg: String) = failure(msg.wrapNel)
}
