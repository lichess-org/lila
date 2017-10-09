package lila.base

import java.lang.Math.{ min, max }
import scala.concurrent.ExecutionContext

import LilaTypes._
import ornicar.scalalib.Zero

final class PimpedBoolean(private val self: Boolean) extends AnyVal {
  /**
   * Replaces scalaz boolean ops
   * so ?? works on Zero and not Monoid
   */
  def ??[A](a: => A)(implicit z: Zero[A]): A = if (self) a else z.zero

  def !(f: => Unit) = if (self) f

  def fold[A](t: => A, f: => A): A = if (self) t else f

  def ?[X](t: => X) = new { def |(f: => X) = if (self) t else f }

  def option[A](a: => A): Option[A] = if (self) Some(a) else None

  def optionFu[A](v: => Fu[A])(implicit ec: ExecutionContext): Fu[Option[A]] =
    if (self) v map { Some(_) } else fuccess(None)
}

final class PimpedLong(private val self: Long) extends AnyVal {

  def atLeast(bottomValue: Long): Long = max(self, bottomValue)

  def atMost(topValue: Long): Long = min(self, topValue)
}

final class PimpedInt(private val self: Int) extends AnyVal {

  def atLeast(bottomValue: Int): Int = max(self, bottomValue)

  def atMost(topValue: Int): Int = min(self, topValue)
}

final class PimpedFloat(private val self: Float) extends AnyVal {

  def atLeast(bottomValue: Float): Float = max(self, bottomValue)

  def atMost(topValue: Float): Float = min(self, topValue)
}

final class PimpedDouble(private val self: Double) extends AnyVal {

  def atLeast(bottomValue: Double): Double = max(self, bottomValue)

  def atMost(topValue: Double): Double = min(self, topValue)
}