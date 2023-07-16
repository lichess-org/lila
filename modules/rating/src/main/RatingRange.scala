package lila.rating

import lila.common.Maths.closestMultipleOf

case class RatingRange(min: IntRating, max: IntRating):

  def contains(rating: IntRating) =
    (min <= RatingRange.min || rating >= min) &&
      (max >= RatingRange.max || rating <= max)

  def notBroad: Option[RatingRange] = (this != RatingRange.broad) option this

  def withinLimits(rating: IntRating, delta: Int) =
    copy(
      min = IntRating(min.atMost(rating + delta).value),
      max = IntRating(max.atLeast(rating - delta).value)
    )

  override def toString = s"$min-$max"

object RatingRange:

  val min = Glicko.minRating
  val max = IntRating(2900)

  val broad   = RatingRange(min, max)
  val default = broad

  private val distribution = Gaussian(1500d, 350d)

  def defaultFor(rating: IntRating) =
    val (rangeMin, rangeMax) = distribution.range(rating.value.toDouble, 0.2)
    RatingRange(IntRating(rangeMin.toInt) atLeast min, IntRating(rangeMax.toInt) atMost max)

  def readRating(str: String) = IntRating from str.toIntOption

  // ^\d{3,4}\-\d{3,4}$
  def apply(from: String): Option[RatingRange] = for
    min <- readRating(from.takeWhile('-' !=))
    if acceptable(min)
    max <- readRating(from.dropWhile('-' !=).tail)
    if acceptable(max)
    if min < max
  yield RatingRange(min, max)

  def apply(rating: IntRating, deltaMin: Option[String], deltaMax: Option[String]): Option[RatingRange] = for
    dmin <- deltaMin.flatMap(_.toIntOption)
    min = rating + dmin
    if acceptable(min)
    dmax <- deltaMax.flatMap(_.toIntOption)
    max = rating + dmax
    if acceptable(max)
    if min < max
  yield RatingRange(min, max)

  def orDefault(from: String) = apply(from) | default
  def orDefault(rating: Option[IntRating], deltaMin: Option[String], deltaMax: Option[String]) =
    rating.flatMap(r => apply(r, deltaMin, deltaMax)) | default

  def noneIfDefault(from: String): Option[RatingRange] =
    if from == default.toString then none
    else apply(from).filter(_ != default)

  def valid(from: String) = apply(from).isDefined

  private def acceptable(rating: IntRating) = broad contains rating
