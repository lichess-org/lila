package lila.coach

sealed abstract class Metric(
  val key: String,
  val name: String,
val position: Position)

object Metric {

  import BSONHandlers._
  import Position._

  case object MeanCpl extends Metric("meanCpl", "Mean CPL", Move)

  case object Movetime extends Metric("movetime", "Move time", Move)

  // case object Result extends Metric("result", "Result", Game)

  case object RatingDiff extends Metric("ratingDiff", "Rating gain", Game)

  case object NbMoves extends Metric("nbMoves", "Number of moves", Move)

  val all = List(MeanCpl, Movetime, RatingDiff, NbMoves)
  def byKey(key: String) = all.find(_.key == key)
}
