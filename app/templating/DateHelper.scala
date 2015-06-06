package lila.app
package templating

import java.util.Locale
import scala.collection.mutable

import org.joda.time.format._
import org.joda.time.{ Period, PeriodType, DurationFieldType, DateTime }
import play.twirl.api.Html

import lila.api.Context

trait DateHelper { self: I18nHelper =>

  private val dateTimeStyle = "MS"
  private val dateStyle = "M-"

  private val dateTimeFormatters = mutable.Map[String, DateTimeFormatter]()
  private val dateFormatters = mutable.Map[String, DateTimeFormatter]()
  private val periodFormatters = mutable.Map[String, PeriodFormatter]()
  private val periodType = PeriodType forFields Array(
    DurationFieldType.days,
    DurationFieldType.hours,
    DurationFieldType.minutes)

  private val isoFormatter = ISODateTimeFormat.dateTime

  private def dateTimeFormatter(ctx: Context): DateTimeFormatter =
    dateTimeFormatters.getOrElseUpdate(
      lang(ctx).language,
      DateTimeFormat forStyle dateTimeStyle withLocale new Locale(lang(ctx).language))

  private def dateFormatter(ctx: Context): DateTimeFormatter =
    dateFormatters.getOrElseUpdate(
      lang(ctx).language,
      DateTimeFormat forStyle dateStyle withLocale new Locale(lang(ctx).language))

  private def periodFormatter(ctx: Context): PeriodFormatter =
    periodFormatters.getOrElseUpdate(
      lang(ctx).language,
      PeriodFormat wordBased new Locale(lang(ctx).language))

  def showDateTime(date: DateTime)(implicit ctx: Context): String =
    dateTimeFormatter(ctx) print date

  def showDate(date: DateTime)(implicit ctx: Context): String =
    dateFormatter(ctx) print date

  def semanticDate(date: DateTime)(implicit ctx: Context) = Html {
    s"""<time datetime="${isoFormatter print date}">${showDate(date)}</time>"""
  }

  def showPeriod(period: Period)(implicit ctx: Context): String =
    periodFormatter(ctx) print period.normalizedStandard(periodType)

  def showMinutes(minutes: Int)(implicit ctx: Context): String =
    showPeriod(new Period(minutes * 60 * 1000l))

  def isoDate(date: DateTime): String = isoFormatter print date

  def momentFormat(date: DateTime, format: String): Html = Html {
    s"""<time class="moment" datetime="${isoFormatter print date}" data-format="$format"></time>"""
  }
  def momentFormat(date: DateTime): Html = momentFormat(date, "calendar")

  def momentFromNow(date: DateTime) = Html {
    s"""<time class="moment-from-now" datetime="${isoFormatter print date}"></time>"""
  }

  def secondsFromNow(seconds: Int) = momentFromNow(DateTime.now plusSeconds seconds)
}
