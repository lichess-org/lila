package lila.base

import java.lang.Math.{ max, min }

import alleycats.Zero

trait LilaPrimitiveExtensions:

  extension [A](self: A)
    def unit: Unit          = ()
    def ap[B](f: A => B): B = f(self)

  extension (self: Long)

    def atLeast(bottomValue: Long): Long = max(self, bottomValue)

    def atMost(topValue: Long): Long = min(self, topValue)

    def squeeze(bottom: Long, top: Long): Long = max(min(self, top), bottom)

    def toSaturatedInt: Int =
      if (self.toInt == self) self.toInt
      else if (self > 0) Integer.MAX_VALUE
      else Integer.MIN_VALUE

  extension (self: Int)

    def atLeast(bottomValue: Int): Int = max(self, bottomValue)

    def atMost(topValue: Int): Int = min(self, topValue)

    def squeeze(bottom: Int, top: Int): Int = max(min(self, top), bottom)

  extension (self: Float)

    def atLeast(bottomValue: Float): Float = max(self, bottomValue)

    def atMost(topValue: Float): Float = min(self, topValue)

    def squeeze(bottom: Float, top: Float): Float = max(min(self, top), bottom)

  extension (self: Double)

    def atLeast(bottomValue: Double): Double = max(self, bottomValue)

    def atMost(topValue: Double): Double = min(self, topValue)

    def squeeze(bottom: Double, top: Double): Double = max(min(self, top), bottom)
