package lila.common

import org.joda.time.{ DateTime, Days }

// number of days since lichess
case class LichessDay(value: Int) extends AnyVal {

  def toDate = LichessDay.genesis.plusDays(value).withTimeAtStartOfDay
}

object LichessDay {

  val genesis = new DateTime(2010, 1, 1, 0, 0).withTimeAtStartOfDay

  def today = LichessDay(Days.daysBetween(genesis, DateTime.now.withTimeAtStartOfDay).getDays)

  def daysAgo(days: Int) = LichessDay(
    Days.daysBetween(genesis, DateTime.now.minusDays(days).withTimeAtStartOfDay).getDays
  )

  def recent(nb: Int): List[LichessDay] =
    (0 until nb).toList.map { delta =>
      LichessDay(Days.daysBetween(genesis, DateTime.now.minusDays(delta).withTimeAtStartOfDay).getDays)
    }

}
