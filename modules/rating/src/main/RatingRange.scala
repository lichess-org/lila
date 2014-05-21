package lila.rating

case class RatingRange(min: Int, max: Int) {

  def contains(rating: Int) = rating >= min && rating <= max

  def notBroad: Option[RatingRange] = (this != RatingRange.broad) option this

  override def toString = "%d-%d".format(min, max)
}

object RatingRange {

  val min = 800
  val max = 2900

  val broad = RatingRange(min, max)
  val default = broad

  // ^\d{3,4}\-\d{3,4}$
  def apply(from: String): Option[RatingRange] = for {
    min ← parseIntOption(from takeWhile ('-' !=))
    if acceptable(min)
    max ← parseIntOption(from dropWhile ('-' !=) tail)
    if acceptable(max)
    if min <= max
  } yield RatingRange(min, max)

  def orDefault(from: String) = apply(from) | default

  def noneIfDefault(from: String) = apply(from) filter (_ != default)

  def valid(from: String) = apply(from).isDefined

  private def acceptable(rating: Int) = broad contains rating
}
