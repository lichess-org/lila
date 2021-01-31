package lila.app
package templating

import java.util.Locale
import org.joda.time.format._
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{ DateTime, DateTimeZone, DurationFieldType, Period, PeriodType }
import play.api.i18n.Lang
import scala.collection.mutable

import lila.app.ui.ScalatagsTemplate._

trait DateHelper { self: I18nHelper with StringHelper =>

  private val dateTimeStyle = "MS"
  private val dateStyle     = "M-"

  private val dateTimeFormatters = mutable.AnyRefMap.empty[String, DateTimeFormatter]
  private val dateFormatters     = mutable.AnyRefMap.empty[String, DateTimeFormatter]
  private val periodFormatters   = mutable.AnyRefMap.empty[String, PeriodFormatter]
  private val periodType = PeriodType forFields Array(
    DurationFieldType.days,
    DurationFieldType.hours,
    DurationFieldType.minutes
  )

  private val isoFormatter = ISODateTimeFormat.dateTime

  private val englishDateFormatter     = DateTimeFormat forStyle dateStyle
  private val englishDateTimeFormatter = DateTimeFormat forStyle dateTimeStyle

  private def dateTimeFormatter(implicit lang: Lang): DateTimeFormatter =
    dateTimeFormatters.getOrElseUpdate(
      lang.code,
      DateTimeFormat forStyle dateTimeStyle withLocale lang.toLocale
    )

  private def dateFormatter(implicit lang: Lang): DateTimeFormatter =
    dateFormatters.getOrElseUpdate(
      lang.code,
      DateTimeFormat forStyle dateStyle withLocale lang.toLocale
    )

  private def periodFormatter(implicit lang: Lang): PeriodFormatter =
    periodFormatters.getOrElseUpdate(
      lang.code, {
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
    timeTag(title := showEnglishDateTime(date))(momentFromNowServerText(date))

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

  private val atomDateFormatter        = ISODateTimeFormat.dateTime
  def atomDate(date: DateTime): String = atomDateFormatter print date
  def atomDate(field: String)(doc: io.prismic.Document): Option[String] =
    doc getDate field map (_.value.toDateTimeAtStartOfDay) map atomDate
}
