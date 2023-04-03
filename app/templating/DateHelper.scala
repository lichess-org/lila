package lila.app
package templating

import java.util.concurrent.ConcurrentHashMap
import play.api.i18n.Lang
import java.time.format.{ FormatStyle, DateTimeFormatter }
import java.time.{ Duration, Period }

import lila.app.ui.ScalatagsTemplate.*
import lila.i18n.PeriodLocales
import chess.format.pgn.Tag.Date

trait DateHelper { self: I18nHelper with StringHelper with NumberHelper =>

  export PeriodLocales.{ showPeriod, showDuration }

  private val dateTimeStyle = "MS"
  private val dateStyle     = "M-"

  private val dateTimeFormatters = new ConcurrentHashMap[String, DateTimeFormatter]
  private val dateFormatters     = new ConcurrentHashMap[String, DateTimeFormatter]

  private val englishDateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
  private val englishDateFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

  private def dateTimeFormatter(using lang: Lang): DateTimeFormatter =
    dateTimeFormatters.computeIfAbsent(
      lang.code,
      _ =>
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(lang.toLocale)
    )

  private def dateFormatter(using lang: Lang): DateTimeFormatter =
    dateFormatters.computeIfAbsent(
      lang.code,
      _ => DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(lang.toLocale)
    )

  def showInstantUTC(date: DateTime)(using Lang): String =
    dateTimeFormatter format date

  def showDate(date: Instant)(using lang: Lang): String =
    if (lang.language == "ar") dateFormatter.format(date).replaceAll("\u200f", "")
    else dateFormatter format date

  def showEnglishDate(date: Instant): String =
    englishDateFormatter format date
  def showEnglishInstant(date: DateTime): String =
    englishDateTimeFormatter format date

  def semanticDate(date: Instant)(using Lang): Tag =
    timeTag(datetimeAttr := isoDate(date))(showDate(date))

  def showMinutes(minutes: Int)(using Lang): String =
    showDuration(Duration.ofMinutes(minutes))

  def isoDate(date: Instant): String = isoDateFormatter format date

  private val oneDayMillis = 1000 * 60 * 60 * 24

  def momentFromNow(date: Instant, alwaysRelative: Boolean = false, once: Boolean = false): Tag =
    if (!alwaysRelative && (date.toMillis - nowMillis) > oneDayMillis) absClientInstant(date)
    else timeTag(cls := s"timeago${once ?? " once"}", datetimeAttr := isoDate(date))(nbsp)

  def momentFromNowWithPreload(date: Instant, alwaysRelative: Boolean = false, once: Boolean = false): Frag =
    momentFromNow(date, alwaysRelative, once)(momentFromNowServerText(date))

  def absClientInstant(date: DateTime): Tag =
    timeTag(cls := "timeago abs", datetimeAttr := isoDate(date))("-")

  def momentFromNowOnce(date: Instant): Tag = momentFromNow(date, once = true)

  def secondsFromNow(seconds: Int, alwaysRelative: Boolean = false): Tag =
    momentFromNow(nowInstant plusSeconds seconds, alwaysRelative)

  def momentFromNowServer(date: Instant): Frag =
    timeTag(title := f"${showEnglishInstant(date)} UTC")(momentFromNowServerText(date))

  def momentFromNowServerText(date: Instant, inFuture: Boolean = false): String =
    val (dateSec, nowSec) = (date.toMillis / 1000, nowSeconds)
    val seconds           = (if inFuture then dateSec - nowSec else nowSec - dateSec).toInt atLeast 0
    val minutes           = seconds / 60
    val hours             = minutes / 60
    val days              = hours / 24
    lazy val weeks        = days / 7
    lazy val months       = days / 30
    lazy val years        = days / 365
    val preposition       = if inFuture then " from now" else " ago"
    if (minutes == 0) "right now"
    else if (hours == 0) s"${pluralize("minute", minutes)}$preposition"
    else if (days < 2) s"${pluralize("hour", hours)}$preposition"
    else if (weeks == 0) s"${pluralize("day", days)}$preposition"
    else if (months == 0) s"${pluralize("week", weeks)}$preposition"
    else if (years == 0) s"${pluralize("month", months)}$preposition"
    else s"${pluralize("year", years)}$preposition"
}
