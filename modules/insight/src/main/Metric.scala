package lila.insight

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
  }

  import BSONHandlers._
  import DataType._
  import Position._

  case object MeanCpl extends Metric("meanCpl", "Mean CPL", Move, Average)

  case object Movetime extends Metric("movetime", "Move time", Move, Seconds)

  // case object Result extends Metric("result", "Result", Game)

  case object RatingDiff extends Metric("ratingDiff", "Rating gain", Game, Average)

  case object NbMoves extends Metric("nbMoves", "Number of moves", Move, Count)

  val all = List(MeanCpl, Movetime, RatingDiff, NbMoves)
  val byKey = all map { p => (p.key, p) } toMap

  def requiresAnalysis(m: Metric) = m match {
    case MeanCpl => true
    case _       => false
  }
}
