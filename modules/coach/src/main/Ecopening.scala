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

  def matchChronoBoardFens(boardFens: List[String]): Option[Ecopening] =
    boardFens.reverse.foldLeft(none[Ecopening]) {
      case (acc, fen) => acc orElse {
        EcopeningDB.all get fen
      }
    }
}
