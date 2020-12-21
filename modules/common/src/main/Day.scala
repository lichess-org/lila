package lila.common

import org.joda.time.{ DateTime, Days, Interval }

// number of days since lichess
case class Day(value: Int) extends AnyVal {

  def toDate = Day.genesis.plusDays(value).withTimeAtStartOfDay
}

object Day {

  val genesis = new DateTime(2010, 1, 1, 0, 0).withTimeAtStartOfDay

  def today = Day(Days.daysBetween(genesis, DateTime.now.withTimeAtStartOfDay).getDays)

  def recent(nb: Int): List[Day] =
    (0 until nb).toList.map { delta =>
      Day(Days.daysBetween(genesis, DateTime.now.minusDays(delta).withTimeAtStartOfDay).getDays)
    }

}
