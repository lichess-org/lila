package lila.insight

import chess.format.FEN
import chess.opening.FullOpeningDB
import chess.{ Color, Role }
import play.api.i18n.Lang
import play.api.libs.json._
import reactivemongo.api.bson._
import scalatags.Text.all._

import lila.common.{ LilaOpening, LilaOpeningFamily }
import lila.db.dsl._
import lila.rating.PerfType

sealed abstract class InsightDimension[A: BSONHandler](
    val key: String,
    val name: String,
    val dbKey: String,
    val position: InsightPosition,
    val description: String
) {

  def bson = implicitly[BSONHandler[A]]

  def isInGame = position == InsightPosition.Game
  def isInMove = position == InsightPosition.Move
}

object InsightDimension {

  import BSONHandlers._
  import InsightPosition._
  import InsightEntry.{ BSONFields => F }
  import lila.rating.BSONHandlers.perfTypeIdHandler

  case object Period
      extends InsightDimension[Period](
        "period",
        "Date",
        F.date,
        Game,
        "The date at which the game was played"
      )

  case object Date
      extends InsightDimension[lila.insight.DateRange](
        "date",
        "Date",
        F.date,
        Game,
        "The date at which the game was played"
      )

  case object Perf
      extends InsightDimension[PerfType](
        "variant",
        "Variant",
        F.perf,
        Game,
        "The rating category of the game, like Bullet, Blitz, or Chess960."
      )

  case object Phase
      extends InsightDimension[Phase](
        "phase",
        "Game phase",
        F.moves("p"),
        Move,
        "The portion of the game: Opening, Middlegame, or Endgame."
      )

  case object Result
      extends InsightDimension[Result](
        "result",
        "Game result",
        F.result,
        Game,
        "Whether you won, lost, or drew the game."
      )

  case object Termination
      extends InsightDimension[Termination](
        "termination",
        "Game termination",
        F.termination,
        Game,
        "The way that the game ended, like Checkmate or Resignation."
      )

  case object Color
      extends InsightDimension[Color](
        "color",
        "Color",
        F.color,
        Game,
        "The side you are playing: White or Black."
      )

  case object OpeningFamily
      extends InsightDimension[LilaOpeningFamily](
        "openingFamily",
        "Opening Family",
        F.openingFamily,
        Game,
        "General opening, like 'Sicilian Defense'."
      )

  case object OpeningVariation
      extends InsightDimension[LilaOpening](
        "openingVariation",
        "Opening Variation",
        F.opening,
        Game,
        "Precise opening, like 'Sicilian Defense: Najdorf Variation'."
      )

  case object OpponentStrength
      extends InsightDimension[RelativeStrength](
        "opponentStrength",
        "Opponent strength",
        F.opponentStrength,
        Game,
        "Rating of your opponent compared to yours. Much weaker:-200, Weaker:-100, Stronger:+100, Much stronger:+200."
      )

  case object PieceRole
      extends InsightDimension[Role](
        "piece",
        "Piece moved",
        F.moves("r"),
        Move,
        "The type of piece you move."
      )

  case object MovetimeRange
      extends InsightDimension[MovetimeRange](
        "movetime",
        "Move time",
        F.moves("t"),
        Move,
        "The amount of time you spend thinking on each move, in seconds."
      )

  case object MyCastling
      extends InsightDimension[Castling](
        "myCastling",
        "My castling side",
        F.myCastling,
        Game,
        "The side you castled on during the game: kingside, queenside, or none."
      )

  case object OpCastling
      extends InsightDimension[Castling](
        "opCastling",
        "Opponent castling side",
        F.opponentCastling,
        Game,
        "The side your opponent castled on during the game: kingside, queenside, or none."
      )

  case object QueenTrade
      extends InsightDimension[QueenTrade](
        "queenTrade",
        "Queen trade",
        F.queenTrade,
        Game,
        "Whether queens were traded before the endgame or not."
      )

  case object MaterialRange
      extends InsightDimension[MaterialRange](
        "material",
        "Material imbalance",
        F.moves("i"),
        Move,
        "Value of your pieces compared to your opponent's. Pawn=1, Bishop/Knight=3, Rook=5, Queen=9."
      )

  case object Blur
      extends InsightDimension[Blur](
        "blur",
        "Move blur",
        F.moves("b"),
        Move,
        "Whether a window blur happened before that move or not."
      )

  case object TimeVariance
      extends InsightDimension[TimeVariance](
        "timeVariance",
        "Time variance",
        F.moves("v"),
        Move,
        "Move time variance. Very consistent: < 0.25, Consistent: < 0.4, Medium, Variable: > 0.6, Very variable > 0.75."
      )

  case object CplRange
      extends InsightDimension[CplRange](
        "cpl",
        "Centipawn loss",
        F.moves("c"),
        Move,
        "Centipawns lost by each move, according to Stockfish evaluation."
      )

  case object EvalRange
      extends InsightDimension[EvalRange](
        "eval",
        "Evaluation",
        F.moves("e"),
        Move,
        "Stockfish evaluation of the position, relative to the player, in centipawns."
      )

  def requiresStableRating(d: InsightDimension[_]) =
    d match {
      case OpponentStrength => true
      case _                => false
    }

  def valuesOf[X](d: InsightDimension[X]): List[X] =
    d match {
      case Period                  => lila.insight.Period.selector
      case Date                    => Nil // Period is used instead
      case Perf                    => PerfType.nonPuzzle
      case Phase                   => lila.insight.Phase.all
      case Result                  => lila.insight.Result.all
      case Termination             => lila.insight.Termination.all
      case Color                   => chess.Color.all
      case OpeningFamily           => LilaOpeningFamily.familyList
      case OpeningVariation        => LilaOpening.openingList
      case OpponentStrength        => RelativeStrength.all
      case PieceRole               => chess.Role.all.reverse
      case MovetimeRange           => lila.insight.MovetimeRange.all
      case CplRange                => lila.insight.CplRange.all
      case MyCastling | OpCastling => lila.insight.Castling.all
      case QueenTrade              => lila.insight.QueenTrade.all
      case MaterialRange           => lila.insight.MaterialRange.all
      case EvalRange               => lila.insight.EvalRange.all
      case Blur                    => lila.insight.Blur.all
      case TimeVariance            => lila.insight.TimeVariance.all
    }

  def valueByKey[X](d: InsightDimension[X], key: String): Option[X] =
    d match {
      case Period                  => key.toIntOption map lila.insight.Period.apply
      case Date                    => None
      case Perf                    => PerfType.byKey get key
      case Phase                   => key.toIntOption flatMap lila.insight.Phase.byId.get
      case Result                  => key.toIntOption flatMap lila.insight.Result.byId.get
      case Termination             => key.toIntOption flatMap lila.insight.Termination.byId.get
      case Color                   => chess.Color fromName key
      case OpeningFamily           => LilaOpeningFamily find key
      case OpeningVariation        => LilaOpening find key
      case OpponentStrength        => key.toIntOption flatMap RelativeStrength.byId.get
      case PieceRole               => chess.Role.all.find(_.name == key)
      case MovetimeRange           => key.toIntOption flatMap lila.insight.MovetimeRange.byId.get
      case CplRange                => key.toIntOption flatMap lila.insight.CplRange.byId.get
      case MyCastling | OpCastling => key.toIntOption flatMap lila.insight.Castling.byId.get
      case QueenTrade              => lila.insight.QueenTrade(key == "true").some
      case MaterialRange           => key.toIntOption flatMap lila.insight.MaterialRange.byId.get
      case EvalRange               => key.toIntOption flatMap lila.insight.EvalRange.byId.get
      case Blur                    => lila.insight.Blur(key == "true").some
      case TimeVariance            => key.toFloatOption map lila.insight.TimeVariance.byId
    }

  def valueToJson[X](d: InsightDimension[X])(v: X)(implicit lang: Lang): play.api.libs.json.JsObject = {
    play.api.libs.json.Json.obj(
      "key"  -> valueKey(d)(v),
      "name" -> valueJson(d)(v)
    )
  }

  def valueKey[X](d: InsightDimension[X])(v: X): String =
    (d match {
      case Date                    => v.toString
      case Period                  => v.days.toString
      case Perf                    => v.key
      case Phase                   => v.id
      case Result                  => v.id
      case Termination             => v.id
      case Color                   => v.name
      case OpeningFamily           => v.key
      case OpeningVariation        => v.key
      case OpponentStrength        => v.id
      case PieceRole               => v.name
      case MovetimeRange           => v.id
      case CplRange                => v.cpl
      case MyCastling | OpCastling => v.id
      case QueenTrade              => v.id
      case MaterialRange           => v.id
      case EvalRange               => v.id
      case Blur                    => v.id
      case TimeVariance            => v.id
    }).toString

  def valueJson[X](d: InsightDimension[X])(v: X)(implicit lang: Lang): JsValue =
    d match {
      case Date                    => JsNumber(v.min.getSeconds)
      case Period                  => JsString(v.toString)
      case Perf                    => JsString(v.trans)
      case Phase                   => JsString(v.name)
      case Result                  => JsString(v.name)
      case Termination             => JsString(v.name)
      case Color                   => JsString(v.toString)
      case OpeningFamily           => JsString(v.name.value)
      case OpeningVariation        => JsString(v.name.value)
      case OpponentStrength        => JsString(v.name)
      case PieceRole               => JsString(v.toString)
      case MovetimeRange           => JsString(v.name)
      case CplRange                => JsString(v.name)
      case MyCastling | OpCastling => JsString(v.name)
      case QueenTrade              => JsString(v.name)
      case MaterialRange           => JsString(v.name)
      case EvalRange               => JsString(v.name)
      case Blur                    => JsString(v.name)
      case TimeVariance            => JsString(v.name)
    }

  def filtersOf[X](d: InsightDimension[X], selected: List[X]): Bdoc = {
    import cats.implicits._
    d match {
      case InsightDimension.MovetimeRange =>
        selected match {
          case Nil => $empty
          case many =>
            $doc(
              "$or" -> many.map(lila.insight.MovetimeRange.toRange).map { range =>
                $doc(d.dbKey $gte range._1 $lt range._2)
              }
            )
        }
      case InsightDimension.Period =>
        selected.maximumByOption(_.days).fold($empty) { period =>
          $doc(d.dbKey $gt period.min)
        }
      case InsightDimension.MaterialRange =>
        selected match {
          case Nil => $empty
          case many =>
            $doc(
              "$or" -> many.map { range =>
                val intRange = lila.insight.MaterialRange.toRange(range)
                if (intRange._1 == intRange._2) $doc(d.dbKey -> intRange._1)
                else if (range.negative)
                  $doc(d.dbKey $gte intRange._1 $lt intRange._2)
                else
                  $doc(d.dbKey $gt intRange._1 $lte intRange._2)
              }
            )
        }
      case InsightDimension.EvalRange =>
        selected match {
          case Nil => $empty
          case many =>
            $doc(
              "$or" -> many.map { range =>
                val intRange = lila.insight.EvalRange.toRange(range)
                if (range.eval < 0)
                  $doc(d.dbKey $gte intRange._1 $lt intRange._2)
                else
                  $doc(d.dbKey $gt intRange._1 $lte intRange._2)
              }
            )
        }
      case InsightDimension.TimeVariance =>
        selected match {
          case Nil => $empty
          case many =>
            $doc(
              "$or" -> many.map(lila.insight.TimeVariance.toRange).map { range =>
                $doc(d.dbKey $gt range._1 $lte range._2)
              }
            )
        }
      case _ =>
        selected flatMap d.bson.writeOpt match {
          case Nil     => $empty
          case List(x) => $doc(d.dbKey -> x)
          case xs      => d.dbKey $in xs
        }
    }
  }

  def requiresAnalysis(d: InsightDimension[_]) =
    d match {
      case CplRange  => true
      case EvalRange => true
      case _         => false
    }

  def dataTypeOf[X](d: InsightDimension[X]): String =
    d match {
      case Date => "date"
      case _    => "text"
    }

  // these are not always present in an insight entry
  val optionalDimensions = List[InsightDimension[_]](OpeningFamily, OpeningVariation, OpponentStrength)
}
