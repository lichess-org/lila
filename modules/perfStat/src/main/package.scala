package lila.perfStat

import chess.IntRating
export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

object percentileOf:
  val group = 25
  import lila.rating.Glicko.minRating
  def apply(ratings: List[Int], rating: IntRating): (Int, Int) =
    ratings.zipWithIndex.foldLeft(0 -> 0) { case ((under, sum), (nb, i)) =>
      {
        if rating > minRating.map(_ + i * group + group / 2)
        then under + nb
        else under
      } -> (sum + nb)
    }
