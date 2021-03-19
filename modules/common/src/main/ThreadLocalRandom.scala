package lila.common

import scala.collection.mutable.StringBuilder

object ThreadLocalRandom {

  import java.util.concurrent.ThreadLocalRandom.current

  def nextBoolean(): Boolean              = current.nextBoolean()
  def nextByte(): Byte                    = current.nextInt(256).toByte
  def nextBytes(bytes: Array[Byte]): Unit = current.nextBytes(bytes)
  def nextDouble(): Double                = current.nextDouble()
  def nextFloat(): Float                  = current.nextFloat()
  def nextInt(): Int                      = current.nextInt()
  def nextInt(n: Int): Int                = current.nextInt(n)
  def nextLong(): Long                    = current.nextLong()
  def nextGaussian(): Double              = current.nextGaussian()
  def nextChar(): Char = {
    val i = nextInt(62)
    if (i < 26) i + 65
    else if (i < 52) i + 71
    else i - 4
  }.toChar
  def shuffle[T, C](xs: IterableOnce[T])(implicit bf: scala.collection.BuildFrom[xs.type, T, C]): C =
    new scala.util.Random(current).shuffle(xs)
  def nextString(len: Int): String = {
    val sb = new StringBuilder(len)
    for (_ <- 0 until len) sb += nextChar()
    sb.result()
  }
  def oneOf[A](vec: Vector[A]): Option[A] =
    vec.nonEmpty ?? {
      vec lift nextInt(vec.size)
    }
}
