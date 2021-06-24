package lila.common

import scala.collection.mutable.StringBuilder

abstract class Random {
  protected def current: java.util.Random

  def nextBoolean(): Boolean              = current.nextBoolean()
  def nextDouble(): Double                = current.nextDouble()
  def nextFloat(): Float                  = current.nextFloat()
  def nextInt(): Int                      = current.nextInt()
  def nextInt(n: Int): Int                = current.nextInt(n)
  def nextLong(): Long                    = current.nextLong()
  def nextGaussian(): Double              = current.nextGaussian()

  def nextBytes(len: Int): Array[Byte] = {
    val bytes = new Array[Byte](len)
    current.nextBytes(bytes)
    bytes
  }

  private def nextAlphanumeric(): Char = {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    chars charAt nextInt(chars.length) // Constant time
  }

  def nextString(len: Int): String = {
    val sb = new StringBuilder(len)
    for (_ <- 0 until len) sb += nextAlphanumeric()
    sb.result()
  }

  def shuffle[T, C](xs: IterableOnce[T])(implicit bf: scala.collection.BuildFrom[xs.type, T, C]): C =
    new scala.util.Random(current).shuffle(xs)

  def oneOf[A](vec: Vector[A]): Option[A] =
    vec.nonEmpty ?? {
      vec lift nextInt(vec.size)
    }
}

object ThreadLocalRandom extends Random {
  override def current = java.util.concurrent.ThreadLocalRandom.current
}

object SecureRandom extends Random {
  override val current = new java.security.SecureRandom().pp("init secure random")
}
