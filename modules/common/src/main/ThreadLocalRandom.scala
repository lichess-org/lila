package lila.common

import scala.collection.mutable.{ ArrayBuffer, StringBuilder }

object ThreadLocalRandom {
  private[this] def current()             = java.util.concurrent.ThreadLocalRandom.current()
  def nextBoolean(): Boolean              = current().nextBoolean()
  def nextBytes(bytes: Array[Byte]): Unit = current().nextBytes(bytes)
  def nextDouble(): Double                = current().nextDouble()
  def nextFloat(): Float                  = current().nextFloat()
  def nextGaussian(): Double              = current().nextGaussian()
  def nextInt(): Int                      = current().nextInt()
  def nextInt(n: Int): Int                = current().nextInt(n)
  def nextLong(): Long                    = current().nextLong()
  def setSeed(seed: Long): Unit           = current().setSeed(seed)
  def nextChar(): Char = {
    val i = nextInt(62)
    (if (i < 26) i + 65
     else if (i < 52) i + 71
     else i - 4).toChar
  }
  def alphanumeric: LazyList[Char] = LazyList.continually(nextChar())

  /** Returns a new collection of the same type in a randomly chosen order.
    *
    *  @return         the shuffled collection
    */
  def shuffle[T, C](xs: IterableOnce[T])(implicit bf: scala.collection.BuildFrom[xs.type, T, C]): C = {
    val buf = new ArrayBuffer[T] ++= xs

    def swap(i1: Int, i2: Int): Unit = {
      val tmp = buf(i1)
      buf(i1) = buf(i2)
      buf(i2) = tmp
    }

    for (n <- buf.length to 2 by -1) {
      val k = nextInt(n)
      swap(n - 1, k)
    }

    (bf.newBuilder(xs) ++= buf).result()
  }
  def nextString(len: Int): String = {
    val sb = new StringBuilder(len)
    for (_ <- 0 until len) sb += nextChar()
    sb.result()
  }
}
