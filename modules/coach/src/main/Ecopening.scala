package lila.coach

case class Ecopening(
    eco: Ecopening.ECO,
    family: Ecopening.Family,
    name: String,
    moves: String,
    fen: String) {

  lazy val moveList = moves.split(' ').toList

  def firstMove = moveList.headOption

  lazy val size = moveList.size

  override def toString = s"$eco $name ($moves)"
}

object Ecopening {
  type Family = String
  type ECO = String

  def apply(game: lila.game.Game): Option[Ecopening] = !game.fromPosition ?? {
    chess.Replay.boards(
      moveStrs = game.pgnMoves take EcopeningDB.MAX_MOVES,
      initialFen = none,
      variant = chess.variant.Standard
    ).toOption flatMap matchChronoBoards
  }

  private def matchChronoBoards(boards: List[chess.Board]): Option[Ecopening] =
    boards.reverse.foldLeft(none[Ecopening]) {
      case (acc, board) => acc orElse {
        EcopeningDB.all get chess.format.Forsyth.exportBoard(board)
      }
    }
}
