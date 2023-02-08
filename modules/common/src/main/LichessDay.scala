package lila.common

import org.joda.time.{ DateTime, Days }

// number of days since lichess
opaque type LichessDay = Int

object LichessDay extends OpaqueInt[LichessDay]:

  extension (day: LichessDay) def toDate = genesis.plusDays(day).withTimeAtStartOfDay

  val genesis = new DateTime(2010, 1, 1, 0, 0).withTimeAtStartOfDay

  def today = LichessDay(Days.daysBetween(genesis, DateTime.now.withTimeAtStartOfDay).getDays)

  def daysAgo(days: Int) = LichessDay(
    Days.daysBetween(genesis, DateTime.now.minusDays(days).withTimeAtStartOfDay).getDays
  )

  def recent(nb: Int): List[LichessDay] =
    (0 until nb).toList.map { delta =>
      LichessDay(Days.daysBetween(genesis, DateTime.now.minusDays(delta).withTimeAtStartOfDay).getDays)
    }
