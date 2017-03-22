package lila.common

import scala.concurrent.duration._

// maximum value = Int.MaxValue / 100 / 60 / 60 / 24 = 248 days
case class Centis(value: Int) extends AnyVal {

  def roundTenths = value / 10
  def millis: Long = value * 10l
  def toDuration = FiniteDuration(millis, MILLISECONDS)

  def +(other: Centis) = Centis(value + other.value)
  def -(other: Centis) = Centis(value - other.value)
  def *(scalar: Int) = Centis(scalar * value)
  def <(other: Centis) = value < other.value
  def >(other: Centis) = value > other.value
  def unary_- = Centis(-value)

  def atMost(other: Int) = Centis(value atMost other)
  def atLeast(other: Int) = Centis(value atLeast other)
}

object Centis {

  def apply(centis: Long): Centis = Centis {
    if (centis > Int.MaxValue) {
      lila.log("common").error(s"Truncating Centis! $centis")
      Int.MaxValue
    }
    else centis.toInt
  }

  def apply(d: FiniteDuration): Centis = Centis {
    if (d.unit eq MILLISECONDS) d.length / 10
    else d.toMillis / 10
  }
}
