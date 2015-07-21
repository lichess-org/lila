package lila.coach

case class RichPov(
  pov: lila.game.Pov,
  initialFen: Option[String],
  analysis: Option[lila.analyse.Analysis],
  division: chess.Division,
  accuracy: Option[lila.analyse.Accuracy.DividedAccuracy],
  moveAccuracy: Option[List[Int]])

case class OpeningFamily(firstMove: String, results: Results, families: List[String])

case class NbSum(nb: Int, sum: Int) {

  def avg = (nb > 0) option (sum / nb)

  def add(v: Int) = copy(nb + 1, sum + v)

  def merge(o: NbSum) = NbSum(nb + o.nb, sum + o.sum)
}
object NbSum {
  val empty = NbSum(0, 0)
}
