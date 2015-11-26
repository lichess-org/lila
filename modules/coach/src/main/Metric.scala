package lila.coach

sealed abstract class Metric(
  val key: String,
  val name: String)

object Metric {

  import BSONHandlers._

  case object MeanCpl extends Metric("meanCpl", "Mean CPL")

  case object Movetime extends Metric("movetime", "Move time")

  // case object Result extends Metric("result", "Result")

  case object RatingDiff extends Metric("ratingDiff", "Rating gain")

  case object NbMoves extends Metric("nbMoves", "Number of moves")

  val all = List(MeanCpl, Movetime, RatingDiff, NbMoves)
  def byKey(key: String) = all.find(_.key == key)
}
