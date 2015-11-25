package lila.coach

sealed abstract class Dimension(val key: String, val name: String)

object Dimension {

  case object Perf extends Dimension("perf", "Variant")
  case object Phase extends Dimension("phase", "Game phase")
  case object Result extends Dimension("result", "Result")
  case object Color extends Dimension("color", "Color")
  case object Opening extends Dimension("opening", "Opening")
  case object OpponentStrength extends Dimension("opponentStrength", "Opponent strength")
  case object PieceRole extends Dimension("pieceRole", "Piece moved")
}
