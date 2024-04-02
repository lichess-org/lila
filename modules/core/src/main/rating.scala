package lila.core
package rating

opaque type PerfKey = String
object PerfKey extends OpaqueString[PerfKey]

opaque type PerfId = Int
object PerfId extends OpaqueInt[PerfId]

trait Perf:
  val glicko: Glicko
  export glicko.{ intRating, intDeviation }

trait Glicko:
  val rating: Double
  val deviation: Double
  val volatility: Double
  def intRating    = IntRating(rating.toInt)
  def intDeviation = deviation.toInt

case class RatingProg(before: IntRating, after: IntRating):
  def diff    = IntRatingDiff(after.value - before.value)
  def isEmpty = diff == IntRatingDiff(0)
case class Score(win: Int, loss: Int, draw: Int, rp: Option[RatingProg]):
  def size = win + loss + draw

case class RatingRange(min: IntRating, max: IntRating):
  def contains(rating: IntRating) =
    (min <= RatingRange.min || rating >= min) &&
      (max >= RatingRange.max || rating <= max)
  override def toString = s"$min-$max"

object RatingRange:

  val min = IntRating(400)
  val max = IntRating(2900)

  val broad   = RatingRange(min, max)
  val default = broad

  def noneIfDefault(from: String): Option[RatingRange] =
    if from == default.toString then none
    else parse(from).filter(_ != default)

  private def readRating(str: String) = IntRating.from(str.toIntOption)

  // ^\d{3,4}\-\d{3,4}$
  def parse(from: String): Option[RatingRange] = for
    min <- readRating(from.takeWhile('-' !=))
    if acceptable(min)
    max <- readRating(from.dropWhile('-' !=).tail)
    if acceptable(max)
    if min < max
  yield RatingRange(min, max)

  def isValid(from: String): Boolean = parse(from).isDefined
  def orDefault(from: String)        = parse(from) | default
  def acceptable(rating: IntRating)  = broad.contains(rating)
