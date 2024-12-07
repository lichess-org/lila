package lila.rating

import chess.IntRating
import lila.core.rating as hub

object RatingRange:

  extension (r: hub.RatingRange)
    def notBroad: Option[hub.RatingRange] = Option.when(r != hub.RatingRange.broad)(r)
    def withinLimits(rating: IntRating, delta: Int) =
      r.copy(
        min = r.min.atMost(rating.map(_ + delta)),
        max = r.max.atLeast(rating.map(_ - delta))
      )

  export hub.RatingRange.*

  private val distribution = Gaussian(1500d, 350d)

  def defaultFor(rating: IntRating) =
    val (rangeMinD, rangeMaxD) = distribution.range(rating.value.toDouble, 0.2)
    val rangeMin: IntRating    = IntRating(rangeMinD.toInt)
    val rangeMax: IntRating    = IntRating(rangeMaxD.toInt)
    hub.RatingRange(rangeMin.atLeast(min), rangeMax.atMost(max))

  def apply(rating: IntRating, deltaMin: Option[String], deltaMax: Option[String]): Option[hub.RatingRange] =
    for
      dmin <- IntRating.from(deltaMin.flatMap(_.toIntOption))
      min = rating + dmin
      if acceptable(min)
      dmax <- IntRating.from(deltaMax.flatMap(_.toIntOption))
      max = rating + dmax
      if acceptable(max)
      if min < max
    yield hub.RatingRange(min, max)

  def orDefault(rating: Option[IntRating], deltaMin: Option[String], deltaMax: Option[String]) =
    rating.flatMap(r => apply(r, deltaMin, deltaMax)) | default
