package lila.common

import scala.collection.BuildFrom
import scala.collection.mutable.{ Growable, PriorityQueue }
import scala.math.Ordering

/*
 * Sorts elements in priority order: higher first, lower last.
 * It's the opposite of what scala .sort does.
 */
object Heapsort:

  private[this] def moveN[T](p: PriorityQueue[T], g: Growable[T], n: Int): Unit =
    // Only the dequeue and dequeueAll methods will return elements in priority order (while removing elements from the heap).
    var k = p.length atMost n
    while k > 0 do
      g += p.dequeue()
      k -= 1

  /* selects maximum nb elements from n size collection
   * should be used for small nb and large n
   * Complexity: O(n + nb * log(n))
   */
  def topN[T, C](xs: IterableOnce[T], nb: Int)(using ord: Ordering[T])(using
      bf: BuildFrom[xs.type, T, C]
  ): C =
    val p = PriorityQueue.from(xs)(ord)
    val b = bf.newBuilder(xs)
    b.sizeHint(p.length atMost nb)
    moveN(p, b, nb)
    b.result()

  def topNToList[T](xs: IterableOnce[T], nb: Int)(using ord: Ordering[T]): List[T] =
    val p = PriorityQueue.from(xs)(ord)
    val b = List.newBuilder[T]
    moveN(p, b, nb)
    b.result()

  extension [A](l: List[A])(using ord: Ordering[A])
    def topN(nb: Int): List[A] = Heapsort.topNToList(l, nb)
    def botN(nb: Int): List[A] = Heapsort.topNToList(l, nb)(using ord.reverse)
