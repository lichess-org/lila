package lila.app
package templating

import java.util.Locale
import play.api.i18n.Lang
import java.util.concurrent.ConcurrentHashMap

import org.joda.time.format._
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{ DateTime, DateTimeZone, DurationFieldType, Period, PeriodType }

import lila.app.ui.ScalatagsTemplate._

trait DateHelper { self: I18nHelper with StringHelper =>

  private val dateTimeStyle = "MS"
  private val dateStyle     = "M-"

  private val dayMonthPattern = "dd-MM"

  private val dateTimeFormatters = new ConcurrentHashMap[String, DateTimeFormatter]
  private val dateFormatters     = new ConcurrentHashMap[String, DateTimeFormatter]
  private val periodFormatters   = new ConcurrentHashMap[String, PeriodFormatter]
  private val periodType = PeriodType forFields Array(
    DurationFieldType.days,
    DurationFieldType.hours,
    DurationFieldType.minutes
  )

  private val isoFormatter = ISODateTimeFormat.dateTime

  private val englishDayMonthFormatter = DateTimeFormat forPattern dayMonthPattern
  private val englishDateFormatter     = DateTimeFormat forStyle dateStyle
  private val englishDateTimeFormatter = DateTimeFormat forStyle dateTimeStyle

  private def dateTimeFormatter(implicit lang: Lang): DateTimeFormatter =
    dateTimeFormatters.computeIfAbsent(
      lang.code,
      _ => DateTimeFormat forStyle dateTimeStyle withLocale lang.toLocale
    )

  private def dateFormatter(implicit lang: Lang): DateTimeFormatter =
    dateFormatters.computeIfAbsent(
      lang.code,
      _ => DateTimeFormat forStyle dateStyle withLocale lang.toLocale
    )

  private def periodFormatter(implicit lang: Lang): PeriodFormatter =
    periodFormatters.computeIfAbsent(
      lang.code,
      _ => {
        Locale setDefault Locale.ENGLISH
        PeriodFormat wordBased lang.toLocale
      }
    )

  def showDateTimeZone(date: DateTime, zone: DateTimeZone)(implicit lang: Lang): String =
    dateTimeFormatter print date.toDateTime(zone)

  def showDateTimeUTC(date: DateTime)(implicit lang: Lang): String =
    showDateTimeZone(date, DateTimeZone.UTC)

  def showDate(date: DateTime)(implicit lang: Lang): String =
    dateFormatter print date

  def showEnglishDayMonth(date: DateTime): String =
    englishDayMonthFormatter print date
  def showEnglishDate(date: DateTime): String =
    englishDateFormatter print date
  def showEnglishDateTime(date: DateTime): String =
    englishDateTimeFormatter print date

  def semanticDate(date: DateTime)(implicit lang: Lang): Frag =
    timeTag(datetimeAttr := isoDate(date))(showDate(date))

  def showPeriod(period: Period)(implicit lang: Lang): String =
    periodFormatter print period.normalizedStandard(periodType)

  def showMinutes(minutes: Int)(implicit lang: Lang): String =
    showPeriod(new Period(minutes * 60 * 1000L))

  def isoDate(date: DateTime): String = isoFormatter print date

  private val oneDayMillis = 1000 * 60 * 60 * 24

  def momentFromNow(date: DateTime, alwaysRelative: Boolean = false, once: Boolean = false): Frag = {
    if (!alwaysRelative && (date.getMillis - nowMillis) > oneDayMillis) absClientDateTime(date)
    else timeTag(cls := s"timeago${once ?? " once"}", datetimeAttr := isoDate(date))(nbsp)
  }

  def absClientDateTime(date: DateTime): Frag =
    timeTag(cls := "timeago abs", datetimeAttr := isoDate(date))("-")

  def momentFromNowOnce(date: DateTime) = momentFromNow(date, once = true)

  def secondsFromNow(seconds: Int, alwaysRelative: Boolean = false) =
    momentFromNow(DateTime.now plusSeconds seconds, alwaysRelative)

  def momentFromNowServer(date: DateTime): Frag = {
    val (dateSec, nowSec) = (date.getMillis / 1000, nowSeconds)
    val seconds           = (nowSec - dateSec) atLeast 0
    val minutes           = seconds / 60
    val hours             = minutes / 60
    val days              = hours / 24
    val years             = days / 365
    val text =
      if (minutes == 0) "Right now"
      else if (hours == 0) s"$minutes minutes ago"
      else if (days == 0) s"$hours hours ago"
      else if (years == 0) s"$days days ago"
      else s"${pluralize("year", years.toInt)} ago"
    timeTag(title := showEnglishDateTime(date))(text)
  }

  private val atomDateFormatter        = ISODateTimeFormat.dateTime
  def atomDate(date: DateTime): String = atomDateFormatter print date
  def atomDate(field: String)(doc: io.prismic.Document): Option[String] =
    doc getDate field map (_.value.toDateTimeAtStartOfDay) map atomDate
}
