package lila.app
package search

import org.elasticsearch.index.query._, FilterBuilders._
import org.joda.time.DateTime

private[search] final class Range[A] private (val a: Option[A], val b: Option[A]) {

  def filters(name: String) = a.fold(b.toList map { bb ⇒ rangeFilter(name) lte bb }) { aa ⇒
    b.fold(List(rangeFilter(name) gte aa)) { bb ⇒
      List(rangeFilter(name) gte aa lte bb)
    }
  }

  def map[B](f: A ⇒ B) = new Range(a map f, b map f)

  def nonEmpty = a.nonEmpty || b.nonEmpty
}

private[search] object Range {

  def apply[A](a: Option[A], b: Option[A])(implicit o: Ordering[A]): Range[A] =
    (a, b) match {
      case (Some(aa), Some(bb)) ⇒ o.lt(aa, bb).fold(
        new Range(a, b), new Range(b, a)
      )
      case (x, y) ⇒ new Range(x, y)
    }

  def none[A]: Range[A] = new Range(None, None)
}
