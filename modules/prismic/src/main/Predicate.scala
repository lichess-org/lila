package lila.prismic

import org.joda.time.DateTime

import lila.prismic.Month.Month
import lila.prismic.WeekDay.WeekDay

object WeekDay extends Enumeration {
  type WeekDay = Value
  val Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday = Value
}

object Month extends Enumeration {
  type Month = Value
  val January, February, March, April, May, June, July, August, September, October, November,
      December = Value
}

trait QuerySerializer[T] {
  def serialize(value: T): String
}

object QuerySerializer {

  def apply[T](f: T => String) = new QuerySerializer[T] {
    override def serialize(value: T): String = f(value)
  }

  implicit val StringSerializer: QuerySerializer[String] = apply("\"" + _ + "\"")

  implicit val DateSerializer: QuerySerializer[DateTime] = apply(_.getMillis.toString)

  implicit val LongSerializer: QuerySerializer[Long] = apply(_.toString)

  implicit val IntSerializer: QuerySerializer[Int] = apply(_.toString)

  implicit val BigDecimalSerializer: QuerySerializer[BigDecimal] = apply(_.toString)

  implicit val WeekDaySerializer: QuerySerializer[WeekDay] = apply("\"" + _ + "\"")

  implicit val MonthSerializer: QuerySerializer[Month] = apply("\"" + _ + "\"")

  implicit def seqSerializer[T](implicit ps: QuerySerializer[T]): QuerySerializer[Seq[T]] =
    new QuerySerializer[Seq[T]] {
      override def serialize(value: Seq[T]): String =
        "[" + value.map(ps.serialize).mkString(",") + "]"
    }

}

sealed trait Predicate {

  def q: String
}

object Predicate {

  import QuerySerializer._

  def apply[T](operator: String, fragment: String, v1: T)(implicit ps: QuerySerializer[T]) =
    new Predicate {
      override def q = s"""[:d = $operator($fragment, ${ps.serialize(v1)})]"""
    }

  def apply[T1, T2](operator: String, fragment: String, v1: T1, v2: T2)(implicit
      ps1: QuerySerializer[T1],
      ps2: QuerySerializer[T2],
  ) = new Predicate {
    override def q = s"""[:d = $operator($fragment, ${ps1.serialize(v1)}, ${ps2.serialize(v2)})]"""
  }

  def apply[T1, T2, T3](operator: String, fragment: String, v1: T1, v2: T2, v3: T3)(implicit
      ps1: QuerySerializer[T1],
      ps2: QuerySerializer[T2],
      ps3: QuerySerializer[T3],
  ) = new Predicate {
    override def q = s"""[:d = $operator($fragment, ${ps1.serialize(v1)}, ${ps2.serialize(
        v2,
      )}, ${ps3.serialize(v3)})]"""
  }

  def at(fragment: String, value: String) = apply("at", fragment, value)

  def any(fragment: String, values: Seq[String]) = apply("any", fragment, values)

  def fulltext(fragment: String, value: String) = apply("fulltext", fragment, value)

  def similar(documentId: String, maxResults: Long) = new Predicate {
    override def q = s"""[:d = similar("$documentId", $maxResults)]"""
  }

  def gt(fragment: String, lowerBound: BigDecimal) = apply("number.gt", fragment, lowerBound)

  def lt(fragment: String, upperBound: BigDecimal) = apply("number.lt", fragment, upperBound)

  def inRange(fragment: String, lowerBound: BigDecimal, upperBound: BigDecimal) =
    apply("number.inRange", fragment, lowerBound, upperBound)

  def dateBefore(fragment: String, before: DateTime) = apply("date.before", fragment, before)

  def dateAfter(fragment: String, after: DateTime) = apply("date.after", fragment, after)

  def dateBetween(fragment: String, before: DateTime, after: DateTime) =
    apply("date.between", fragment, before, after)

  def dayOfMonth(fragment: String, day: Int) = apply("date.day-of-month", fragment, day)

  def dayOfMonthAfter(fragment: String, day: Int) = apply("date.day-of-month-after", fragment, day)

  def dayOfMonthBefore(fragment: String, day: Int) =
    apply("date.day-of-month-before", fragment, day)

  def dayOfWeek(fragment: String, day: WeekDay) = apply("date.day-of-week", fragment, day)

  def dayOfWeekBefore(fragment: String, day: WeekDay) =
    apply("date.day-of-week-before", fragment, day)

  def dayOfWeekAfter(fragment: String, day: WeekDay) =
    apply("date.day-of-week-after", fragment, day)

  def month(fragment: String, month: Month) = apply("date.month", fragment, month)

  def monthBefore(fragment: String, month: Month) = apply("date.month-before", fragment, month)

  def monthAfter(fragment: String, month: Month) = apply("date.month-after", fragment, month)

  def year(fragment: String, year: Int) = apply("date.year", fragment, year)

  def hour(fragment: String, hour: Int) = apply("date.hour", fragment, hour)

  def hourBefore(fragment: String, hour: Int) = apply("date.hour-before", fragment, hour)

  def hourAfter(fragment: String, hour: Int) = apply("date.hour-after", fragment, hour)

  def near(fragment: String, latitude: BigDecimal, longitude: BigDecimal, radius: Int) =
    apply("geopoint.near", fragment, latitude, longitude, radius)

}
