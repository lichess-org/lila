package lila.common

import java.time.LocalDateTime

// number of days since lichess
opaque type LichessDay = Int

object LichessDay extends OpaqueInt[LichessDay]:

  extension (day: LichessDay) def toDate = genesis.plusDays(day).withTimeAtStartOfDay

  val genesis: Instant = LocalDateTime.of(2010, 1, 1, 0, 0).withTimeAtStartOfDay.instant

  def dayOf(time: Instant) = LichessDay(daysBetween(genesis, time))

  def today = dayOf(nowInstant)

  def daysAgo(days: Int) = dayOf(nowInstant.minusDays(days))

  def recent(nb: Int): List[LichessDay] =
    (0 until nb).toList.map { delta =>
      dayOf(nowInstant.minusDays(delta))
    }
