package lila.rating

import chess.ByColor
import chess.rating.glicko.Glicko

object RatingRegulator:

  private val factors = Map(
    PerfKey.rapid -> 1.015,
    PerfKey.classical -> 1.015,
    PerfKey.blitz -> 1.005,
    PerfKey.bullet -> 1.015,
    PerfKey.ultraBullet -> 1.013,
    PerfKey.atomic -> 1.02,
    PerfKey.antichess -> 1.02
  )

  def apply(
      key: PerfKey,
      before: ByColor[Glicko],
      after: ByColor[Glicko],
      isBot: ByColor[Boolean]
  ): ByColor[Glicko] =
    val regulated = before.zip(after, (b, a) => regulate(key, b, a))
    val halvedAgainstBot = regulated.mapWithColor: (color, glicko) =>
      if !isBot(color) && isBot(!color)
      then glicko.average(before(color))
      else glicko
    halvedAgainstBot

  private def regulate(key: PerfKey, before: Glicko, after: Glicko): Glicko =
    factors
      .get(key)
      .filter(_ != 1)
      .fold(after):
        regulate(_, key, before, after)

  private def regulate(factor: Double, key: PerfKey, before: Glicko, after: Glicko): Glicko =
    if after.rating > before.rating
    then
      val diff = after.rating - before.rating
      val extra = diff * (factor - 1)
      lila.mon.rating.regulator.micropoints(key.value).record((extra * 1000 * 1000).toLong)
      after.copy(rating = after.rating + extra)
    else after
