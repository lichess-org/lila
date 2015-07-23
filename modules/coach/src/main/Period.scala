package lila.coach

import org.joda.time.DateTime
import scalaz.NonEmptyList

// contains aggregated data over (up to) 100 games
case class Period(
    _id: String,
    userId: String,
    data: UserStat,
    from: DateTime,
    to: DateTime,
    computedAt: DateTime) {

  def merge(o: Period) = copy(
    data = data merge o.data,
    from = from,
    to = o.to)
}

case class Periods(periods: NonEmptyList[Period]) {

  lazy val period: Period = periods.tail.foldLeft(periods.head)(_ merge _)
}
