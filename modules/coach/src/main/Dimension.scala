package lila.coach

import reactivemongo.bson._

import chess.{ Color, Role }
import lila.db.Types._
import lila.rating.PerfType

sealed abstract class Dimension[A: BSONValueHandler](
    val key: String,
    val name: String,
    val dbKey: String,
    val position: Dimension.Position) {

  implicit def bson = implicitly[BSONValueHandler[A]]

  def isInGame = position == Dimension.Game
  def isInMove = position == Dimension.Move
}

object Dimension {

  sealed trait Position
  case object Game extends Position
  case object Move extends Position

  import BSONHandlers._

  case object Perf extends Dimension[PerfType]("perf", "Variant", "perf", Game)

  case object Phase extends Dimension[Phase]("phase", "Game phase", "moves.p", Move)

  case object Result extends Dimension[Result]("result", "Result", "result", Game)

  case object Color extends Dimension[Color]("color", "Color", "color", Game)

  case object Opening extends Dimension[Ecopening]("opening", "Opening", "eco", Game)

  case object OpponentStrength extends Dimension[RelativeStrength]("opponentStrength", "Opponent strength", "opponent.strength", Game)

  case object PieceRole extends Dimension[Role]("pieceRole", "Piece moved", "moves.r", Move)
}
