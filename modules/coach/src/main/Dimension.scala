package lila.coach

import reactivemongo.bson._

import chess.{ Color, Role }
import lila.db.Types._
import lila.rating.PerfType

sealed abstract class Dimension[A: BSONValueHandler](
    val key: String,
    val name: String,
    val dbKey: String,
    val position: Position,
    val valueName: A => String) {

  implicit def bson = implicitly[BSONValueHandler[A]]

  def isInGame = position == Position.Game
  def isInMove = position == Position.Move
}

object Dimension {

  import BSONHandlers._
  import Position._

  case object Perf extends Dimension[PerfType](
    "perf", "Variant", "perf", Game, _.name)

  case object Phase extends Dimension[Phase](
    "phase", "Game phase", "moves.p", Move, _.name)

  case object Result extends Dimension[Result](
    "result", "Result", "result", Game, _.name)

  case object Color extends Dimension[Color](
    "color", "Color", "color", Game, _.name)

  case object Opening extends Dimension[Ecopening](
    "opening", "Opening", "eco", Game, _.ecoName)

  case object OpponentStrength extends Dimension[RelativeStrength](
    "opponentStrength", "Opponent strength", "opponent.strength", Game, _.name)

  case object PieceRole extends Dimension[Role](
    "pieceRole", "Piece moved", "moves.r", Move, _.name)

  val all = List(Perf, Phase, Result, Color, Opening, OpponentStrength, PieceRole)
  def byKey(key: String) = all.find(_.key == key)

  def valuesOf[X](d: Dimension[X]): List[X] = d match {
    case Perf             => PerfType.nonPuzzle
    case Phase            => lila.coach.Phase.all
    case Result           => lila.coach.Result.all
    case Color            => chess.Color.all
    case Opening          => EcopeningDB.all
    case OpponentStrength => RelativeStrength.all
    case PieceRole        => chess.Role.all
  }

  def valueToJson[X](d: Dimension[X])(v: X): play.api.libs.json.JsObject = {
    import play.api.libs.json._
    def toJson[A : Writes](key: A, name: String) = Json.obj("key" -> key, "name" -> name)
    d match {
      case Perf             => toJson(v.key, v.name)
      case Phase            => toJson(v.id, v.name)
      case Result           => toJson(v.id, v.name)
      case Color            => toJson(v.name, v.name)
      case Opening          => toJson(v.eco, v.name)
      case OpponentStrength => toJson(v.id, v.name)
      case PieceRole        => toJson(v.name, v.name)
    }
  }
}
