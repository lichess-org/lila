package lila.rating

import lila.common.Maths.closestMultipleOf

case class RatingRange(min: Int, max: Int) {

  def contains(rating: Int) =
    (min <= RatingRange.min || rating >= min) &&
      (max >= RatingRange.max || rating <= max)

  def notBroad: Option[RatingRange] = (this != RatingRange.broad) option this

  def withinLimits(rating: Int, delta: Int, multipleOf: Int) =
    copy(
      min = closestMultipleOf(multipleOf, min.atMost(rating + delta)),
      max = closestMultipleOf(multipleOf, max.atLeast(rating - delta))
    )

  override def toString = s"$min-$max"
}

object RatingRange {

  val min = Glicko.minRating
  val max = 2900

  val broad   = RatingRange(min, max)
  val default = broad

  // ^\d{3,4}\-\d{3,4}$
  def apply(from: String): Option[RatingRange] =
    for {
      min <- from.takeWhile('-' !=).toIntOption
      if acceptable(min)
      max <- from.dropWhile('-' !=).tail.toIntOption
      if acceptable(max)
      if min < max
    } yield RatingRange(min, max)

  def orDefault(from: String)         = apply(from) | default
  def orDefault(from: Option[String]) = from.flatMap(apply) | default

  def noneIfDefault(from: String) =
    if (from == default.toString) none
    else apply(from).filter(_ != default)

  def valid(from: String) = apply(from).isDefined

  private def acceptable(rating: Int) = broad contains rating
}
