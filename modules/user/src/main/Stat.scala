package lila.user

object Stat {

  val group = 25

  def percentile(ratings: List[Int], rating: Int): (Int, Int) =
    ratings.zipWithIndex.foldLeft(0 -> 0) {
      case ((under, sum), (nb, i)) => {
        if ((800 + i * group) < (rating + group / 2)) under + nb else under
      } -> (sum + nb)
    }
}
