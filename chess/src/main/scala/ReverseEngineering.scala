package lila.chess

final class ReverseEngineering(from: Game, to: Game) {

  def move: Option[(Pos, Pos)] =
    if (to.turns != from.turns + 1) None else findMove

  private def findMove: Option[(Pos, Pos)] = {
    findMovedPieces match {
      case List((pos, piece)) ⇒ findPieceNewPos(pos, piece) map { np ⇒ (pos, np) }
      case _                  ⇒ None
    }
  }

  private def findPieceNewPos(pos: Pos, piece: Piece): Option[Pos] = for {
    dests ← from.situation.destinations get pos
    dest ← dests find { to.board(_) map (_ is piece.color) getOrElse false }
  } yield dest

  private def findMovedPieces: List[(Pos, Piece)] = {

    val fromPlayerPieces = from.board piecesOf from.player
    val toPlayerPieces = to.board piecesOf from.player

    fromPlayerPieces map {
      case (pos, piece) ⇒ (pos, piece, toPlayerPieces get pos)
    } collect {
      case (pos, piece, None)                                ⇒ pos -> piece
      case (pos, piece, Some(newPiece)) if piece != newPiece ⇒ pos -> piece
    } toList
  }
}
