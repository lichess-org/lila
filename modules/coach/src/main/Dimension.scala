package lila.coach

import lila.rating.PerfType
import chess.{Color,Role}

sealed abstract class Dimension[A](val key: String, val name: String)

object Dimension {

  case object Perf extends Dimension[PerfType]("perf", "Variant")
  case object Phase extends Dimension[Phase]("phase", "Game phase")
  case object Result extends Dimension[Result]("result", "Result")
  case object Color extends Dimension[Color]("color", "Color")
  case object Opening extends Dimension[Ecopening]("opening", "Opening")
  case object OpponentStrength extends Dimension[RelativeStrength]("opponentStrength", "Opponent strength")
  case object PieceRole extends Dimension[Role]("pieceRole", "Piece moved")
}
