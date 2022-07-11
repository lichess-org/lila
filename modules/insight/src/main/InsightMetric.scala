package lila.insight

import scalatags.Text.all._

import reactivemongo.api.bson._

sealed abstract class InsightMetric(
    val key: String,
    val name: String,
    val dbKey: String,
    val position: InsightPosition,
    val per: InsightPosition,
    val dataType: InsightMetric.DataType,
    val description: String
)

object InsightMetric {

  sealed trait DataType {
    def name = toString.toLowerCase
  }
  object DataType {
    case object Seconds extends DataType
    case object Count   extends DataType
    case object Average extends DataType
    case object Percent extends DataType
  }

  import DataType._
  import InsightPosition._
  import InsightEntry.{ BSONFields => F }

  case object MeanCpl
      extends InsightMetric(
        "acpl",
        "Average centipawn loss",
        F moves "c",
        Move,
        Move,
        Average,
        """Precision of your moves. Lower is better."""
      )

  case object CplBucket
      extends InsightMetric(
        "cplBucket",
        "Centipawn loss bucket",
        F moves "c",
        Move,
        Move,
        Percent,
        InsightDimension.CplRange.description
      )

  case object MeanAccuracy
      extends InsightMetric(
        "accuracy",
        "Accuracy",
        F moves "a",
        Move,
        Move,
        Percent,
        InsightDimension.AccuracyPercentRange.description
      )

  case object Movetime
      extends InsightMetric(
        "movetime",
        "Move time",
        F moves "t",
        Move,
        Move,
        Seconds,
        InsightDimension.MovetimeRange.description
      )

  case object Result
      extends InsightMetric(
        "result",
        "Game result",
        F.result,
        Game,
        Game,
        Percent,
        InsightDimension.Result.description
      )

  case object Termination
      extends InsightMetric(
        "termination",
        "Game termination",
        F.termination,
        Game,
        Game,
        Percent,
        InsightDimension.Termination.description
      )

  case object Performance
      extends InsightMetric(
        "performance",
        "Performance",
        F.opponentRating,
        Game,
        Game,
        Average,
        "Estimated performance rating."
      )

  case object RatingDiff
      extends InsightMetric(
        "ratingDiff",
        "Rating gain",
        F.ratingDiff,
        Game,
        Game,
        Average,
        "The amount of rating points you win or lose when the game ends."
      )

  case object OpponentRating
      extends InsightMetric(
        "opponentRating",
        "Opponent rating",
        F.opponentRating,
        Game,
        Game,
        Average,
        "The average rating of your opponent for the relevant variant."
      )

  case object NbMoves
      extends InsightMetric(
        "nbMoves",
        "Moves per game",
        F moves "r",
        Move,
        Game,
        Average,
        "Number of moves you play in the game. Doesn't count the opponent moves."
      )

  case object PieceRole
      extends InsightMetric(
        "piece",
        "Piece moved",
        F moves "r",
        Move,
        Move,
        Percent,
        InsightDimension.PieceRole.description
      )

  case object Awareness
      extends InsightMetric(
        "awareness",
        "Tactical awareness",
        F moves "o",
        Move,
        Move,
        Percent,
        "How often you take advantage of your opponent mistakes."
      )

  case object Luck
      extends InsightMetric(
        "luck",
        "Luck",
        F moves "l",
        Move,
        Move,
        Percent,
        "How often your opponent fails to punish your mistakes."
      )

  case object Material
      extends InsightMetric(
        "material",
        "Material imbalance",
        F moves "i",
        Move,
        Move,
        Average,
        InsightDimension.MaterialRange.description
      )

  case object ClockPercent
      extends InsightMetric(
        "clockPercent",
        "Time pressure",
        F moves "s",
        Move,
        Move,
        Average,
        InsightDimension.ClockPercentRange.description
      )

  case object Blurs
      extends InsightMetric(
        "blurs",
        "Blurs",
        F moves "b",
        Move,
        Move,
        Percent,
        "How often moves are preceded by a window blur."
      )

  case object TimeVariance
      extends InsightMetric(
        "timeVariance",
        "Time variance",
        F moves "v",
        Move,
        Move,
        Average,
        "Low variance means consistent move times"
      )

  val all = List(
    MeanCpl,
    CplBucket,
    MeanAccuracy,
    Movetime,
    ClockPercent,
    Result,
    Termination,
    Performance,
    RatingDiff,
    OpponentRating,
    NbMoves,
    PieceRole,
    Awareness,
    Luck,
    Material,
    Blurs,
    TimeVariance
  )
  val byKey = all map { p =>
    (p.key, p)
  } toMap

  def requiresAnalysis(m: InsightMetric) = m match {
    case MeanCpl | CplBucket | MeanAccuracy => true
    case _                                  => false
  }

  def requiresStableRating(m: InsightMetric) = m match {
    case Performance | RatingDiff | OpponentRating => true
    case _                                         => false
  }

  def isStacked(m: InsightMetric) = m match {
    case Result      => true
    case Termination => true
    case PieceRole   => true
    case CplBucket   => true
    case _           => false
  }

  def valuesOf(metric: InsightMetric): List[MetricValue] = metric match {
    case Result =>
      lila.insight.Result.all.map { r =>
        MetricValue(BSONInteger(r.id), MetricValueName(r.name))
      }
    case Termination =>
      lila.insight.Termination.all.map { r =>
        MetricValue(BSONInteger(r.id), MetricValueName(r.name))
      }
    case PieceRole =>
      chess.Role.all.reverse.map { r =>
        MetricValue(BSONString(r.forsyth.toString), MetricValueName(r.toString))
      }
    case CplBucket =>
      lila.insight.CplRange.all.map { cpl =>
        MetricValue(BSONInteger(cpl.cpl), MetricValueName(cpl.name))
      }
    case _ => Nil
  }

  case class MetricValueName(name: String) extends AnyVal
  case class MetricValue(key: BSONValue, name: MetricValueName)
}
