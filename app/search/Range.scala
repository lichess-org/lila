package lila
package search

import org.elasticsearch.index.query._, FilterBuilders._

case class Range[A](a: Option[A], b: Option[A]) {

  def filters(name: String) = a.fold(
    aa ⇒ b.fold(
      bb ⇒ List(rangeFilter(name) gte aa lte bb),
      List(rangeFilter(name) gte aa)
    ),
    b.toList map { bb ⇒ rangeFilter(name) lte bb }
  )

  def map[B](f: A ⇒ B) = Range(a map f, b map f)

  def nonEmpty = a.nonEmpty || b.nonEmpty
}

object Range {

  def none[A]: Range[A] = Range(None, None)
}
