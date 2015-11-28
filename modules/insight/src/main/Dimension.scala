package lila.insight

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
    "color", "Color", "color", Game, _.toString)

  case object Opening extends Dimension[Ecopening](
    "opening", "Opening", "eco", Game, _.ecoName)

  case object OpponentStrength extends Dimension[RelativeStrength](
    "opponentStrength", "Opponent strength", "opponent.strength", Game, _.name)

  case object PieceRole extends Dimension[Role](
    "pieceRole", "Piece moved", "moves.r", Move, _.toString)

  // case object Castling extends Dimension[Castling](
  //   "castling", "Castling side", "moves.c", Move, _.name)

  val all = List(Perf, Phase, Result, Color, Opening, OpponentStrength, PieceRole)
  val byKey = all map { p => (p.key, p) } toMap

  def valuesOf[X](d: Dimension[X]): List[X] = d match {
    case Perf             => PerfType.nonPuzzle
    case Phase            => lila.insight.Phase.all
    case Result           => lila.insight.Result.all
    case Color            => chess.Color.all
    case Opening          => EcopeningDB.all
    case OpponentStrength => RelativeStrength.all
    case PieceRole        => chess.Role.all.reverse
  }

  def valueByKey[X](d: Dimension[X], key: String): Option[X] = d match {
    case Perf             => PerfType.byKey get key
    case Phase            => parseIntOption(key) flatMap lila.insight.Phase.byId.get
    case Result           => parseIntOption(key) flatMap lila.insight.Result.byId.get
    case Color            => chess.Color(key)
    case Opening          => EcopeningDB.allByEco get key
    case OpponentStrength => parseIntOption(key) flatMap RelativeStrength.byId.get
    case PieceRole        => chess.Role.all.find(_.name == key)
  }

  def valueToJson[X](d: Dimension[X])(v: X): play.api.libs.json.JsObject = {
    import play.api.libs.json._
    def toJson[A: Writes](key: A) = Json.obj(
      "key" -> key.toString,
      "name" -> d.valueName(v))
    d match {
      case Perf             => toJson(v.key)
      case Phase            => toJson(v.id)
      case Result           => toJson(v.id)
      case Color            => toJson(v.name)
      case Opening          => toJson(v.eco)
      case OpponentStrength => toJson(v.id)
      case PieceRole        => toJson(v.name)
    }
  }
}
