package lila.common

import scala.collection.BuildFrom
import scala.collection.mutable.{ Growable, PriorityQueue }

object Heapsort {
  private[this] def moveN[T](p: PriorityQueue[T], g: Growable[T], n: Int): Unit = {
    //Only the dequeue and dequeueAll methods will return elements in priority order (while removing elements from the heap).
    var k = n
    while (k > 0 && p.nonEmpty) {
      g += p.dequeue()
      k -= 1
    }
  }
  /* selects maximum nb elements from n size collection
   * should be used for small nb and large n
   * Complexity: O(n + nb * log(n))
   */
  def topN[T, C](xs: IterableOnce[T], nb: Int, ord: scala.math.Ordering[T])(implicit
      bf: BuildFrom[xs.type, T, C]
  ): C = {
    val p = PriorityQueue.from(xs)(ord)
    val b = bf.newBuilder(xs)
    b.sizeHint(p.length atMost nb)
    moveN(p, b, nb)
    b.result()
  }
  def topNToList[T, C](xs: IterableOnce[T], nb: Int, ord: scala.math.Ordering[T]): List[T] = {
    val p = PriorityQueue.from(xs)(ord)
    val b = List.newBuilder[T]
    moveN(p, b, nb)
    b.result()
  }
  class ListOps[A](private val l: List[A]) extends AnyVal {
    def topN(nb: Int)(implicit ord: scala.math.Ordering[A]): List[A] =
      Heapsort.topNToList(l, nb, ord)
  }
  implicit def toListOps[A](l: List[A]) = new ListOps(l)
}
