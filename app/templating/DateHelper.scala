package lila.app
package templating

import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import org.joda.time.format.{ DateTimeFormat, DateTimeFormatter }
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{ DateTime, DateTimeZone, DurationFieldType, Period }
import play.api.i18n.Lang
import scala.collection.mutable

import lila.app.ui.ScalatagsTemplate._
import lila.i18n.PeriodLocales

trait DateHelper { self: I18nHelper with StringHelper =>

  private val dateTimeStyle = "MS"
  private val dateStyle     = "M-"

  private val dateTimeFormatters = new ConcurrentHashMap[String, DateTimeFormatter]
  private val dateFormatters     = new ConcurrentHashMap[String, DateTimeFormatter]

  private val isoFormatter = ISODateTimeFormat.dateTime

  private val englishDateFormatter     = DateTimeFormat forStyle dateStyle
  private val englishDateTimeFormatter = DateTimeFormat forStyle dateTimeStyle

  private def dateTimeFormatter(implicit lang: Lang): DateTimeFormatter =
    dateTimeFormatters.computeIfAbsent(
      lang.code,
      _ => DateTimeFormat.forStyle(dateTimeStyle).withLocale(lang.toLocale)
    )

  private def dateFormatter(implicit lang: Lang): DateTimeFormatter =
    dateFormatters.computeIfAbsent(
      lang.code,
      _ => DateTimeFormat.forStyle(dateStyle).withLocale(lang.toLocale)
    )

  def showDateTimeZone(date: DateTime, zone: DateTimeZone)(implicit lang: Lang): String =
    dateTimeFormatter print date.toDateTime(zone)

  def showDateTimeUTC(date: DateTime)(implicit lang: Lang): String =
    showDateTimeZone(date, DateTimeZone.UTC)

  def showDate(date: DateTime)(implicit lang: Lang): String =
    if (lang.language == "ar") dateFormatter print date replaceAll ("\u200f", "")
    else dateFormatter print date

  def showEnglishDate(date: DateTime): String =
    englishDateFormatter print date
  def showEnglishDateTime(date: DateTime): String =
    englishDateTimeFormatter print date

  def semanticDate(date: DateTime)(implicit lang: Lang): Tag =
    timeTag(datetimeAttr := isoDate(date))(showDate(date))

  def showPeriod(period: Period)(implicit lang: Lang): String =
    PeriodLocales.showPeriod(period)

  def showMinutes(minutes: Int)(implicit lang: Lang): String =
    showPeriod(new Period(minutes * 60 * 1000L))

  def isoDate(date: DateTime): String = isoFormatter print date

  private val oneDayMillis = 1000 * 60 * 60 * 24

  def momentFromNow(date: DateTime, alwaysRelative: Boolean = false, once: Boolean = false): Tag = {
    if (!alwaysRelative && (date.getMillis - nowMillis) > oneDayMillis) absClientDateTime(date)
    else timeTag(cls := s"timeago${once ?? " once"}", datetimeAttr := isoDate(date))(nbsp)
  }

  def momentFromNowWithPreload(date: DateTime, alwaysRelative: Boolean = false, once: Boolean = false): Frag =
    momentFromNow(date, alwaysRelative, once)(momentFromNowServerText(date))

  def absClientDateTime(date: DateTime): Tag =
    timeTag(cls := "timeago abs", datetimeAttr := isoDate(date))("-")

  def momentFromNowOnce(date: DateTime) = momentFromNow(date, once = true)

  def secondsFromNow(seconds: Int, alwaysRelative: Boolean = false) =
    momentFromNow(DateTime.now plusSeconds seconds, alwaysRelative)

  def momentFromNowServer(date: DateTime): Frag =
    timeTag(title := f"${showEnglishDateTime(date)} UTC")(momentFromNowServerText(date))

  def momentFromNowServerText(date: DateTime): Frag = {
    val (dateSec, nowSec) = (date.getMillis / 1000, nowSeconds)
    val seconds           = (nowSec - dateSec).toInt atLeast 0
    val minutes           = seconds / 60
    val hours             = minutes / 60
    val days              = hours / 24
    lazy val weeks        = days / 7
    lazy val months       = days / 30
    lazy val years        = days / 365
    if (minutes == 0) "right now"
    else if (hours == 0) s"${pluralize("minute", minutes)} ago"
    else if (days < 2) s"${pluralize("hour", hours)} ago"
    else if (weeks == 0) s"${pluralize("day", days)} ago"
    else if (months == 0) s"${pluralize("week", weeks)} ago"
    else if (years == 0) s"${pluralize("month", months)} ago"
    else s"${pluralize("year", years)} ago"
  }
}
