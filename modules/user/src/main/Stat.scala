package lila.user

object Stat {

  val minRating = lila.rating.Glicko.minRating
  val group = 25

  def percentile(ratings: List[Int], rating: Int): (Int, Int) =
    ratings.zipWithIndex.foldLeft(0 -> 0) {
      case ((under, sum), (nb, i)) => {
        if ((minRating + i * group) < (rating + group / 2)) under + nb else under
      } -> (sum + nb)
    }
}
