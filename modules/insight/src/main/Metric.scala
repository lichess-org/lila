package lila.insight

import reactivemongo.bson._

sealed abstract class Metric(
  val key: String,
  val name: String,
  val position: Position,
  val dataType: Metric.DataType)

object Metric {

  sealed trait DataType {
    def name = toString.toLowerCase
  }
  object DataType {
    case object Seconds extends DataType
    case object Count extends DataType
    case object Average extends DataType
    case object Percent extends DataType
  }

  import BSONHandlers._
  import DataType._
  import Position._

  case object MeanCpl extends Metric("meanCpl", "Average centipawn loss", Move, Average)

  case object Movetime extends Metric("movetime", "Move time", Move, Seconds)

  case object Result extends Metric("result", "Result", Game, Percent)

  case object Termination extends Metric("termination", "Termination", Game, Percent)

  case object RatingDiff extends Metric("ratingDiff", "Rating gain", Game, Average)

  case object OpponentRating extends Metric("opponentRating", "Opponent rating", Game, Average)

  case object NbMoves extends Metric("nbMoves", "Moves per game", Game, Count)

  case object PieceRole extends Metric("pieceRole", "Piece moved", Move, Percent)

  val all = List(MeanCpl, Movetime, Result, Termination, RatingDiff, OpponentRating, NbMoves, PieceRole)
  val byKey = all map { p => (p.key, p) } toMap

  def requiresAnalysis(m: Metric) = m match {
    case MeanCpl => true
    case _       => false
  }

  def isStacked(m: Metric) = m match {
    case Result      => true
    case Termination => true
    case PieceRole   => true
    case _           => false
  }

  def valuesOf(metric: Metric): List[MetricValue] = metric match {
    case Result => lila.insight.Result.all.map { r =>
      MetricValue(BSONInteger(r.id), MetricValueName(r.name))
    }
    case Termination => lila.insight.Termination.all.map { r =>
      MetricValue(BSONInteger(r.id), MetricValueName(r.name))
    }
    case PieceRole => chess.Role.all.reverse.map { r =>
      MetricValue(BSONString(r.forsyth.toString), MetricValueName(r.toString))
    }
    case _ => Nil
  }

  case class MetricValueName(name: String)
  case class MetricValue(key: BSONValue, name: MetricValueName)
}
