package lila.ublog

import java.time.{ Year, YearMonth, ZoneOffset, LocalTime }

import scala.util.Try

import lila.db.dsl.{ *, given }

object UblogByMonth:

  private val ublogOrigin = YearMonth.of(2021, 9)

  private def currentYearMonth = YearMonth.now(ZoneOffset.UTC)
  def allYears = (ublogOrigin.getYear to currentYearMonth.getYear).toList

  def selector(month: YearMonth) =
    val (start, until) = boundsOfMonth(month)
    // to hit topic prod index
    $doc("lived.at".$gt(start).$lt(until))

  private def boundsOfMonth(month: YearMonth): (Instant, Instant) =
    val start = month.atDay(1).atStartOfDay()
    val until = month.atEndOfMonth().atTime(LocalTime.MAX)
    (start.toInstant(ZoneOffset.UTC), until.toInstant(ZoneOffset.UTC))

  def readYear(year: Int): Option[Year] =
    (ublogOrigin.getYear <= year && year <= currentYearMonth.getYear).so(Try(Year.of(year)).toOption)

  def isValid(ym: YearMonth): Boolean =
    // writing it as negative allow bounds to be included
    !(ym.isBefore(ublogOrigin) || ym.isAfter(currentYearMonth))

  def readYearMonth(year: Int, month: Int): Option[YearMonth] =
    Try(YearMonth.of(year, month)).toOption
