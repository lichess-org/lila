package lila.search

final class Range[A] private (val a: Option[A], val b: Option[A]) {

  def map[B](f: A => B) = new Range(a map f, b map f)

  def nonEmpty = a.nonEmpty || b.nonEmpty
}

object Range {

  import play.api.libs.json._

  implicit def rangeJsonWriter[A: Writes] =
    Writes[Range[A]] { r =>
      Json.obj("a" -> r.a, "b" -> r.b)
    }

  def apply[A](a: Option[A], b: Option[A])(implicit o: Ordering[A]): Range[A] =
    (a, b) match {
      case (Some(aa), Some(bb)) =>
        if (o.lt(aa, bb)) new Range(a, b)
        else new Range(b, a)
      case (x, y) => new Range(x, y)
    }

  def none[A]: Range[A] = new Range(None, None)
}
