package lila.coach

case class Ecopening(
    eco: Ecopening.ECO,
    family: Ecopening.FamilyName,
    name: String,
    moves: String,
    fen: Ecopening.FEN,
    lastMoveUci: String) {

  lazy val moveList = moves.split(' ').toList

  def firstMove = moveList.headOption

  lazy val size = moveList.size

  lazy val formattedMoves: String =
    moveList.grouped(2).zipWithIndex.map {
      case (List(w, b), i) => s"${i + 1}. $w $b"
      case (List(w), i)    => s"${i + 1}. $w"
      case _               => ""
    }.mkString(" ")

  override def toString = s"$eco $name ($moves)"
}

object Ecopening {
  type FamilyName = String
  type ECO = String
  type FEN = String

  case class Family(name: FamilyName, ecos: List[FEN])
  def makeFamilies(ops: Iterable[Ecopening]): Map[FamilyName, Family] =
    ops.foldLeft(Map.empty[FamilyName, Family]) {
      case (fams, op) => fams + (op.family -> fams.get(op.family).fold(Family(op.family, List(op.eco))) {
        existing => existing.copy(ecos = op.eco :: existing.ecos)
      })
    }

  def fromGame(game: lila.game.Game): Option[Ecopening] =
    lila.game.Game.canGuessOpening(game) ?? {
      chess.Replay.boards(
        moveStrs = game.pgnMoves take EcopeningDB.MAX_MOVES,
        initialFen = none,
        variant = chess.variant.Standard
      ).toOption flatMap matchChronoBoards
    }

  private def matchChronoBoards(boards: List[chess.Board]): Option[Ecopening] =
    boards.reverse.foldLeft(none[Ecopening]) {
      case (acc, board) => acc orElse {
        EcopeningDB.allByFen get chess.format.Forsyth.exportBoard(board)
      }
    }
}
