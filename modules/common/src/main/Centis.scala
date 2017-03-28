package lila.common

import scala.concurrent.duration._

// maximum value = Int.MaxValue / 100 / 60 / 60 / 24 = 248 days
case class Centis(value: Int) extends AnyVal with Ordered[Centis] {

  def roundTenths: Int = math.round(value / 10f)
  def roundSeconds: Int = math.round(value / 100f)
  def millis: Long = value * 10l
  def toDuration = FiniteDuration(millis, MILLISECONDS)

  def +(other: Centis) = Centis(value + other.value)
  def -(other: Centis) = Centis(value - other.value)
  def *(scalar: Int) = Centis(scalar * value)
  def unary_- = Centis(-value)

  def compare(other: Centis) = value compare other.value

  def abs: Centis = Centis(value.abs)
  def atMost(other: Int) = Centis(value atMost other)
  def atLeast(other: Int) = Centis(value atLeast other)
}

object Centis {

  def apply(centis: Long): Centis = Centis {
    if (centis > Int.MaxValue) {
      lila.log("common").error(s"Truncating Centis! $centis")
      Int.MaxValue
    }
    else if (centis < Int.MinValue) {
      lila.log("common").error(s"Truncating Centis! $centis")
      Int.MinValue
    }
    else centis.toInt
  }

  def apply(d: FiniteDuration): Centis = Centis {
    if (d.unit eq MILLISECONDS) d.length / 10
    else d.toMillis / 10
  }
}
