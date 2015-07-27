package lila.rating

object Regulator {

  def apply(before: Perf, after: Perf) =
    if (before.nb >= after.nb) after
    else {
      val diff = (after.glicko.rating - before.glicko.rating).abs
      val extra = diff / 100
      after.copy(
        glicko = after.glicko.copy(
          rating = after.glicko.rating + extra))
    }
}
