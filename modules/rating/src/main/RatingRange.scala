package lila.rating

import lila.core.rating as hub

object RatingRange:

  extension (r: hub.RatingRange)
    def notBroad: Option[hub.RatingRange] = Option.when(r != hub.RatingRange.broad)(r)
    def withinLimits(rating: IntRating, delta: Int) =
      r.copy(
        min = IntRating(r.min.atMost(rating + delta).value),
        max = IntRating(r.max.atLeast(rating - delta).value)
      )

  export hub.RatingRange.*

  private val distribution = Gaussian(1500d, 350d)

  def defaultFor(rating: IntRating) =
    val (rangeMin, rangeMax) = distribution.range(rating.value.toDouble, 0.2)
    hub.RatingRange(IntRating(rangeMin.toInt).atLeast(min), IntRating(rangeMax.toInt).atMost(max))

  def apply(rating: IntRating, deltaMin: Option[String], deltaMax: Option[String]): Option[hub.RatingRange] =
    for
      dmin <- deltaMin.flatMap(_.toIntOption)
      min = rating + dmin
      if acceptable(min)
      dmax <- deltaMax.flatMap(_.toIntOption)
      max = rating + dmax
      if acceptable(max)
      if min < max
    yield hub.RatingRange(min, max)

  def orDefault(rating: Option[IntRating], deltaMin: Option[String], deltaMax: Option[String]) =
    rating.flatMap(r => apply(r, deltaMin, deltaMax)) | default
