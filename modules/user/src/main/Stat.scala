package lila.user

object Stat:

  export lila.rating.Glicko.minRating
  val group = 25

  def percentile(ratings: List[Int], rating: IntRating): (Int, Int) =
    ratings.zipWithIndex.foldLeft(0 -> 0) { case ((under, sum), (nb, i)) =>
      {
        if rating > minRating.value + i * group + group / 2 then under + nb else under
      } -> (sum + nb)
    }
