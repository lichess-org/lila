package lila.rating

object RatingRegulator {

  def apply(factors: RatingFactors)(perfType: PerfType, before: Perf, after: Perf): Perf =
    factors.get(perfType).filter(1 !=).fold(after) {
      apply(_, perfType, before, after)
    }

  private def apply(factor: RatingFactor, perfType: PerfType, before: Perf, after: Perf): Perf =
    if ({
      (after.nb == before.nb + 1) &&               // after playing one game
      (after.glicko.rating > before.glicko.rating) // and gaining rating
    }) {
      val diff  = after.glicko.rating - before.glicko.rating
      val extra = diff * (factor.value - 1)
      lila.mon.rating.regulator.micropoints(perfType.key).record((extra * 1000 * 1000).toLong)
      after.copy(
        glicko = after.glicko.copy(
          rating = after.glicko.rating + extra
        )
      )
    } else after
}
