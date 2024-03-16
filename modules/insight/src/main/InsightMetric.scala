package lila.insight

import reactivemongo.api.bson.*

import InsightMetric.DataType.*
import InsightPosition.*
import InsightEntry.BSONFields as F

enum InsightMetric(
    val key: String,
    val name: String,
    val dbKey: String,
    val position: InsightPosition,
    val per: InsightPosition,
    val dataType: InsightMetric.DataType,
    val description: String
):

  case MeanCpl
      extends InsightMetric(
        "acpl",
        "Average centipawn loss",
        F.moves("c"),
        Move,
        Move,
        Average,
        """Precision of your moves. Lower is better."""
      )

  case CplBucket
      extends InsightMetric(
        "cplBucket",
        "Centipawn loss bucket",
        F.moves("c"),
        Move,
        Move,
        Percent,
        InsightDimension.CplRange.description
      )

  case MeanAccuracy
      extends InsightMetric(
        "accuracy",
        "Accuracy",
        F.moves("a"),
        Move,
        Move,
        Percent,
        InsightDimension.AccuracyPercentRange.description
      )

  case Movetime
      extends InsightMetric(
        "movetime",
        "Move time",
        F.moves("t"),
        Move,
        Move,
        Seconds,
        InsightDimension.MovetimeRange.description
      )

  case Result
      extends InsightMetric(
        "result",
        "Game result",
        F.result,
        Game,
        Game,
        Percent,
        InsightDimension.Result.description
      )

  case Termination
      extends InsightMetric(
        "termination",
        "Game termination",
        F.termination,
        Game,
        Game,
        Percent,
        InsightDimension.Termination.description
      )

  case Performance
      extends InsightMetric(
        "performance",
        "Performance",
        F.opponentRating,
        Game,
        Game,
        Average,
        "Estimated performance rating."
      )

  case RatingDiff
      extends InsightMetric(
        "ratingDiff",
        "Rating gain",
        F.ratingDiff,
        Game,
        Game,
        Average,
        "The amount of rating points you win or lose when the game ends."
      )

  case OpponentRating
      extends InsightMetric(
        "opponentRating",
        "Opponent rating",
        F.opponentRating,
        Game,
        Game,
        Average,
        "The average rating of your opponent for the relevant variant."
      )

  case NbMoves
      extends InsightMetric(
        "nbMoves",
        "Moves per game",
        F.moves("r"),
        Move,
        Game,
        Average,
        "Number of moves you play in the game. Doesn't count the opponent moves."
      )

  case PieceRole
      extends InsightMetric(
        "piece",
        "Piece moved",
        F.moves("r"),
        Move,
        Move,
        Percent,
        InsightDimension.PieceRole.description
      )

  case Awareness
      extends InsightMetric(
        "awareness",
        "Tactical awareness",
        F.moves("o"),
        Move,
        Move,
        Percent,
        "How often you take advantage of your opponent mistakes."
      )

  case Luck
      extends InsightMetric(
        "luck",
        "Luck",
        F.moves("l"),
        Move,
        Move,
        Percent,
        "How often your opponent fails to punish your mistakes."
      )

  case Material
      extends InsightMetric(
        "material",
        "Material imbalance",
        F.moves("i"),
        Move,
        Move,
        Average,
        InsightDimension.MaterialRange.description
      )

  case ClockPercent
      extends InsightMetric(
        "clockPercent",
        "Time pressure",
        F.moves("s"),
        Move,
        Move,
        Average,
        InsightDimension.ClockPercentRange.description
      )

  case Blurs
      extends InsightMetric(
        "blurs",
        "Blurs",
        F.moves("b"),
        Move,
        Move,
        Percent,
        "How often moves are preceded by a window blur."
      )

  case TimeVariance
      extends InsightMetric(
        "timeVariance",
        "Time variance",
        F.moves("v"),
        Move,
        Move,
        Average,
        "Low variance means consistent move times"
      )

object InsightMetric:

  enum DataType:
    case Seconds, Count, Average, Percent
    def name = toString.toLowerCase

  val byKey = values.mapBy(_.key)

  val requiresAnalysis     = Set(MeanCpl, CplBucket, MeanAccuracy, Awareness)
  val requiresStableRating = Set(Performance, RatingDiff, OpponentRating)
  val isStacked            = Set(Result, Termination, PieceRole, CplBucket)

  def valuesOf(metric: InsightMetric): Seq[MetricValue] = metric match
    case Result =>
      lila.insight.Result.values.map { r =>
        MetricValue(BSONInteger(r.id), MetricValueName(r.name))
      }.toIndexedSeq
    case Termination =>
      lila.insight.Termination.values.map { r =>
        MetricValue(BSONInteger(r.id), MetricValueName(r.name))
      }.toIndexedSeq
    case PieceRole =>
      chess.Role.all.reverse.map { r =>
        MetricValue(BSONString(r.forsyth.toString), MetricValueName(r.toString))
      }
    case CplBucket =>
      lila.insight.CplRange.all.map { cpl =>
        MetricValue(BSONInteger(cpl.cpl), MetricValueName(cpl.name))
      }
    case _ => Nil

  opaque type MetricValueName = String
  object MetricValueName extends OpaqueString[MetricValueName]

  case class MetricValue(key: BSONValue, name: MetricValueName)
