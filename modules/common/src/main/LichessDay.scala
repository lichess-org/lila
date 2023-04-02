package lila.common

import java.time.LocalDateTime

// number of days since lichess
opaque type LichessDay = Int

object LichessDay extends OpaqueInt[LichessDay]:

  extension (day: LichessDay) def toDate = genesis.plusDays(day).withTimeAtStartOfDay

  val genesis = LocalDateTime.of(2010, 1, 1, 0, 0).withTimeAtStartOfDay

  def dayOf(time: LocalDateTime) = LichessDay(daysBetween(genesis, time))

  def today = dayOf(nowDate)

  def daysAgo(days: Int) = dayOf(nowDate.minusDays(days))

  def recent(nb: Int): List[LichessDay] =
    (0 until nb).toList.map { delta =>
      dayOf(nowDate.minusDays(delta))
    }
