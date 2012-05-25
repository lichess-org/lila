package lila
package elo

case class EloRange(min: Int, max: Int) {

  override def toString = "%d-%d".format(min, max)
}

object EloRange {

  val min = 800
  val max = 2200

  val default = EloRange(min, max)

  // ^\d{3,4}\-\d{3,4}$
  def apply(from: String): Option[EloRange] = for {
    min ← parseIntOption(from takeWhile ('-' !=))
    if acceptable(min)
    max ← parseIntOption(from dropWhile ('-' !=) tail)
    if acceptable(max)
    if min <= max
  } yield EloRange(min, max)

  def orDefault(from: String) = apply(from) | default

  def noneIfDefault(from: String) = apply(from) filter (_ != default)

  def valid(from: String) = apply(from).isDefined

  private def acceptable(v: Int) = v >= min && v <= max
}
