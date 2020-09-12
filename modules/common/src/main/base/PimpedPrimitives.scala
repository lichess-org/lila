package lila.base

import java.lang.Math.{ max, min }

import ornicar.scalalib.Zero

final class PimpedBoolean(private val self: Boolean) extends AnyVal {

  /**
    * Replaces scalaz boolean ops
    * so ?? works on Zero and not Monoid
    */
  def ??[A](a: => A)(implicit z: Zero[A]): A = if (self) a else z.zero

  def option[A](a: => A): Option[A] = if (self) Some(a) else None
}

final class PimpedLong(private val self: Long) extends AnyVal {

  def atLeast(bottomValue: Long): Long = max(self, bottomValue)

  def atMost(topValue: Long): Long = min(self, topValue)

  def squeeze(bottom: Long, top: Long): Long = max(min(self, top), bottom)

  def toSaturatedInt: Int =
    if (self.toInt == self) self.toInt
    else if (self > 0) Integer.MAX_VALUE
    else Integer.MIN_VALUE
}

final class PimpedInt(private val self: Int) extends AnyVal {

  def atLeast(bottomValue: Int): Int = max(self, bottomValue)

  def atMost(topValue: Int): Int = min(self, topValue)

  def squeeze(bottom: Int, top: Int): Int = max(min(self, top), bottom)
}

final class PimpedFloat(private val self: Float) extends AnyVal {

  def atLeast(bottomValue: Float): Float = max(self, bottomValue)

  def atMost(topValue: Float): Float = min(self, topValue)

  def squeeze(bottom: Float, top: Float): Float = max(min(self, top), bottom)
}

final class PimpedDouble(private val self: Double) extends AnyVal {

  def atLeast(bottomValue: Double): Double = max(self, bottomValue)

  def atMost(topValue: Double): Double = min(self, topValue)

  def squeeze(bottom: Double, top: Double): Double = max(min(self, top), bottom)
}
