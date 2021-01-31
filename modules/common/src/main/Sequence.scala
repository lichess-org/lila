package lila.common

object Sequence {
  def interleave[A](a: Seq[A], b: Seq[A]): Vector[A] = {
    val iterA   = a.iterator
    val iterB   = b.iterator
    val builder = Vector.newBuilder[A]
    while (iterA.hasNext && iterB.hasNext) {
      builder += iterA.next() += iterB.next()
    }
    builder ++= iterA ++= iterB

    builder.result()
  }
}
