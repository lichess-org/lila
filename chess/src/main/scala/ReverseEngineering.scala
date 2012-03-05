package lila.chess

final class ReverseEngineering(fromGame: Game, to: Board) {

  val from = fromGame.board

  def move: Option[(Pos, Pos)] =
    if (from.pieces == to.pieces) None else findMove

  private def findMove: Option[(Pos, Pos)] = {
    findMovedPieces match {
      case List((pos, piece)) ⇒ findPieceNewPos(pos, piece) map { np ⇒ (pos, np) }
      case _                  ⇒ None
    }
  }

  private def findPieceNewPos(pos: Pos, piece: Piece): Option[Pos] = for {
    dests ← fromGame.situation.destinations get pos
    dest ← dests find { to(_) map (_ is piece.color) getOrElse false }
  } yield dest

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
}
