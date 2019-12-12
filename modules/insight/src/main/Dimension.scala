package lila.insight

import scalatags.Text.all._
import reactivemongo.bson._
import play.api.libs.json._

import chess.opening.EcopeningDB
import chess.{ Color, Role }
import lila.db.dsl._
import lila.rating.PerfType

sealed abstract class Dimension[A: BSONValueHandler](
    val key: String,
    val name: String,
    val dbKey: String,
    val position: Position,
    val description: Frag
) {

  def bson = implicitly[BSONValueHandler[A]]

  def isInGame = position == Position.Game
  def isInMove = position == Position.Move
}

object Dimension {

  import BSONHandlers._
  import Position._
  import Entry.{ BSONFields => F }
  import lila.rating.BSONHandlers.perfTypeIdHandler

  case object Period extends Dimension[Period](
    "period", "Date", F.date, Game,
    raw("The date at which the game was played")
  )

  case object Date extends Dimension[lila.insight.DateRange](
    "date", "Date", F.date, Game,
    raw("The date at which the game was played")
  )

  case object Perf extends Dimension[PerfType](
    "variant", "Variant", F.perf, Game,
    raw("The rating category of the game, like Bullet, Blitz, or Chess960.")
  )

  case object Phase extends Dimension[Phase](
    "phase", "Game phase", F.moves("p"), Move,
    raw("The portion of the game: Opening, Middlegame, or Endgame.")
  )

  case object Result extends Dimension[Result](
    "result", "Game result", F.result, Game,
    raw("Whether you won, lost, or drew the game.")
  )

  case object Termination extends Dimension[Termination](
    "termination", "Game termination", F.termination, Game,
    raw("The way that the game ended, like Checkmate or Resignation.")
  )

  case object Color extends Dimension[Color](
    "color", "Color", F.color, Game,
    raw("The side you are playing: White or Black.")
  )

  case object Opening extends Dimension[chess.opening.Ecopening](
    "opening", "Opening", F.eco, Game,
    raw("ECO identification of the initial moves, like \"A58 Benko Gambit\".")
  )

  case object OpponentStrength extends Dimension[RelativeStrength](
    "opponentStrength", "Opponent strength", F.opponentStrength, Game,
    raw("Rating of your opponent compared to yours. Much weaker:-200, Weaker:-100, Stronger:+100, Much stronger:+200.")
  )

  case object PieceRole extends Dimension[Role](
    "piece", "Piece moved", F.moves("r"), Move,
    raw("The type of piece you move.")
  )

  case object MovetimeRange extends Dimension[MovetimeRange](
    "movetime", "Move time", F.moves("t"), Move,
    raw("The amount of time you spend thinking on each move, in seconds.")
  )

  case object MyCastling extends Dimension[Castling](
    "myCastling", "My castling side", F.myCastling, Game,
    raw("The side you castled on during the game: kingside, queenside, or none.")
  )

  case object OpCastling extends Dimension[Castling](
    "opCastling", "Opponent castling side", F.opponentCastling, Game,
    raw("The side your opponent castled on during the game: kingside, queenside, or none.")
  )

  case object QueenTrade extends Dimension[QueenTrade](
    "queenTrade", "Queen trade", F.queenTrade, Game,
    raw("Whether queens were traded before the endgame or not.")
  )

  case object MaterialRange extends Dimension[MaterialRange](
    "material", "Material imbalance", F.moves("i"), Move,
    raw("Value of your pieces compared to your opponent's. Pawn=1, Bishop/Knight=3, Rook=5, Queen=9.")
  )

  def requiresStableRating(d: Dimension[_]) = d match {
    case OpponentStrength => true
    case _ => false
  }

  def valuesOf[X](d: Dimension[X]): List[X] = d match {
    case Period => lila.insight.Period.selector
    case Date => Nil // Period is used instead
    case Perf => PerfType.nonPuzzle
    case Phase => lila.insight.Phase.all
    case Result => lila.insight.Result.all
    case Termination => lila.insight.Termination.all
    case Color => chess.Color.all
    case Opening => EcopeningDB.all
    case OpponentStrength => RelativeStrength.all
    case PieceRole => chess.Role.all.reverse
    case MovetimeRange => lila.insight.MovetimeRange.all
    case MyCastling | OpCastling => lila.insight.Castling.all
    case QueenTrade => lila.insight.QueenTrade.all
    case MaterialRange => lila.insight.MaterialRange.all
  }

  def valueByKey[X](d: Dimension[X], key: String): Option[X] = d match {
    case Period => parseIntOption(key) map lila.insight.Period.apply
    case Date => None
    case Perf => PerfType.byKey get key
    case Phase => parseIntOption(key) flatMap lila.insight.Phase.byId.get
    case Result => parseIntOption(key) flatMap lila.insight.Result.byId.get
    case Termination => parseIntOption(key) flatMap lila.insight.Termination.byId.get
    case Color => chess.Color(key)
    case Opening => EcopeningDB.allByEco get key
    case OpponentStrength => parseIntOption(key) flatMap RelativeStrength.byId.get
    case PieceRole => chess.Role.all.find(_.name == key)
    case MovetimeRange => parseIntOption(key) flatMap lila.insight.MovetimeRange.byId.get
    case MyCastling | OpCastling => parseIntOption(key) flatMap lila.insight.Castling.byId.get
    case QueenTrade => lila.insight.QueenTrade(key == "true").some
    case MaterialRange => parseIntOption(key) flatMap lila.insight.MaterialRange.byId.get
  }

  def valueToJson[X](d: Dimension[X])(v: X): play.api.libs.json.JsObject = {
    play.api.libs.json.Json.obj(
      "key" -> valueKey(d)(v),
      "name" -> valueJson(d)(v)
    )
  }

  def valueKey[X](d: Dimension[X])(v: X): String = (d match {
    case Date => v.toString
    case Period => v.days.toString
    case Perf => v.key
    case Phase => v.id
    case Result => v.id
    case Termination => v.id
    case Color => v.name
    case Opening => v.eco
    case OpponentStrength => v.id
    case PieceRole => v.name
    case MovetimeRange => v.id
    case MyCastling | OpCastling => v.id
    case QueenTrade => v.id
    case MaterialRange => v.id
  }).toString

  def valueJson[X](d: Dimension[X])(v: X): JsValue = d match {
    case Date => JsNumber(v.min.getSeconds)
    case Period => JsString(v.toString)
    case Perf => JsString(v.name)
    case Phase => JsString(v.name)
    case Result => JsString(v.name)
    case Termination => JsString(v.name)
    case Color => JsString(v.toString)
    case Opening => JsString(v.ecoName)
    case OpponentStrength => JsString(v.name)
    case PieceRole => JsString(v.toString)
    case MovetimeRange => JsString(v.name)
    case MyCastling | OpCastling => JsString(v.name)
    case QueenTrade => JsString(v.name)
    case MaterialRange => JsString(v.name)
  }

  def filtersOf[X](d: Dimension[X], selected: List[X]): Bdoc = d match {
    case Dimension.MovetimeRange => selected match {
      case Nil => $empty
      case xs => $doc(d.dbKey $in xs.flatMap(_.tenths.toList))
    }
    case Dimension.Period => selected.sortBy(-_.days).headOption.fold($empty) { period =>
      $doc(d.dbKey $gt period.min)
    }
    case _ => selected map d.bson.write match {
      case Nil => $empty
      case List(x) => $doc(d.dbKey -> x)
      case xs => $doc(d.dbKey -> $doc("$in" -> BSONArray(xs)))
    }
  }

  def dataTypeOf[X](d: Dimension[X]): String = d match {
    case Date => "date"
    case _ => "text"
  }
}
