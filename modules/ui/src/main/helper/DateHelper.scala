package lila.ui

import play.api.i18n.Lang

import java.time.format.{ DateTimeFormatter, FormatStyle, TextStyle }
import java.time.{ Duration, LocalDate, Month, YearMonth }

import lila.core.i18n.{ maxLangs, Translate, I18nKey }
import lila.ui.ScalatagsTemplate.*
import scalalib.model.Seconds

trait DateHelper:
  self: StringHelper =>

  val datetimeAttr = attr("datetime")

  private val dateTimeFormatters = scalalib.ConcurrentMap[String, DateTimeFormatter](maxLangs)
  private val dateFormatters     = scalalib.ConcurrentMap[String, DateTimeFormatter](maxLangs)

  private val englishDateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
  private val englishDateFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

  private def dateTimeFormatter(using lang: Lang): DateTimeFormatter =
    dateTimeFormatters.computeIfAbsentAlways(lang.code):
      DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(lang.toLocale)

  private def dateFormatter(using lang: Lang): DateTimeFormatter =
    dateFormatters.computeIfAbsentAlways(lang.code):
      DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(lang.toLocale)

  private val englishTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

  def showTime(time: Instant)(using Translate): Tag =
    timeTag(title := s"${showInstant(time)} UTC"):
      englishTimeFormatter.format(time.dateTime)

  def showInstant(instant: Instant)(using t: Translate): String =
    dateTimeFormatter(using t.lang).print(instant)

  def showDate(instant: Instant)(using Translate): String =
    showDate(instant.date)

  def showDate(date: LocalDate)(using t: Translate): String =
    given lang: Lang = t.lang
    if lang.language == "ar"
    then dateFormatter.print(date).replaceAll("\u200f", "")
    else dateFormatter.print(date)

  def showYearMonth(month: YearMonth)(using lang: Lang): String =
    val formatter = DateTimeFormatter.ofPattern("yyyy/MM").withLocale(lang.toLocale)
    month.format(formatter)

  def showMonth(m: Month)(using lang: Lang): String =
    m.getDisplayName(TextStyle.FULL, lang.toLocale)

  def showEnglishDate(instant: Instant): String    = englishDateFormatter.print(instant)
  def showEnglishInstant(instant: Instant): String = englishDateTimeFormatter.print(instant)

  def semanticDate(instant: Instant)(using t: Translate): Tag =
    timeTag(datetimeAttr := isoDateTime(instant))(showDate(instant))

  def semanticDate(date: LocalDate)(using t: Translate): Tag =
    timeTag(datetimeAttr := isoDateTime(date.atStartOfDay.instant))(showDate(date))

  def showMinutes(minutes: Int)(using Translate): String =
    lila.core.i18n.translateDuration(Duration.ofMinutes(minutes))

  def isoDateTime(instant: Instant): String = isoDateTimeFormatter.print(instant)

  private val oneDayMillis = 1000 * 60 * 60 * 24

  def momentFromNow(instant: Instant)(using Translate): Tag = momentFromNow(instant, false, false)

  def momentFromNow(instant: Instant, alwaysRelative: Boolean = false, once: Boolean = false)(using Translate): Tag =
    val nowMillis = System.currentTimeMillis()
    if !alwaysRelative && (instant.toMillis - nowMillis) > oneDayMillis then
      absClientInstantEmpty(instant)(nbsp)
    else
      timeTag(cls := s"timeago${once.so(" once")}", datetimeAttr := isoDateTime(instant))(
        momentFromNowServerText(instant)
      )

  def momentFromNowWithPreload(instant: Instant)(using Translate): Frag =
    momentFromNowWithPreload(instant, false, false)

  def momentFromNowWithPreload(
      instant: Instant,
      alwaysRelative: Boolean = false,
      once: Boolean = false
  )(using Translate): Frag =
    momentFromNow(instant, alwaysRelative, once)(momentFromNowServerText(instant))

  def absClientInstant(instant: Instant)(using Translate): Tag =
    absClientInstantEmpty(instant)(showInstant(instant))

  private def absClientInstantEmpty(instant: Instant): Tag =
    timeTag(cls := "timeago abs", datetimeAttr := isoDateTime(instant))

  def momentFromNowOnce(instant: Instant)(using Translate): Tag = momentFromNow(instant, once = true)

  def secondsFromNow(seconds: Seconds, alwaysRelative: Boolean = false)(using Translate): Tag =
    momentFromNow(nowInstant.plusSeconds(seconds.value), alwaysRelative)

  def momentFromNowServer(instant: Instant)(using Translate): Frag =
    timeTag(title := s"${showInstant(instant)} UTC")(momentFromNowServerText(instant))

  def momentFromNowServerText(instant: Instant)(using t: Translate): String =
    val nowSeconds = System.currentTimeMillis() / 1000
    val dateSec    = instant.toMillis / 1000
    val inFuture   = dateSec > nowSeconds
    val seconds    = (if inFuture then dateSec - nowSeconds else nowSeconds - dateSec).toInt.atLeast(0)
    val minutes    = seconds / 60
    val hours      = minutes / 60
    val days       = hours / 24
    val weeks      = days / 7
    val months     = days / 30
    val years      = days / 365

    def plural(keyBase: String, count: Int): String =
      val key = keyBase match
        case "second" => if inFuture then I18nKey.timeago.inNbSeconds else I18nKey.timeago.nbSecondsAgo
        case "minute" => if inFuture then I18nKey.timeago.inNbMinutes else I18nKey.timeago.nbMinutesAgo
        case "hour"   => if inFuture then I18nKey.timeago.inNbHours else I18nKey.timeago.nbHoursAgo
        case "day"    => if inFuture then I18nKey.timeago.inNbDays else I18nKey.timeago.nbDaysAgo
        case "week"   => if inFuture then I18nKey.timeago.inNbWeeks else I18nKey.timeago.nbWeeksAgo
        case "month"  => if inFuture then I18nKey.timeago.inNbMonths else I18nKey.timeago.nbMonthsAgo
        case "year"   => if inFuture then I18nKey.timeago.inNbYears else I18nKey.timeago.nbYearsAgo
        case _        => I18nKey.timeago.justNow

      key.pluralSameTxt(count)

    if seconds < 5 then
      if inFuture then I18nKey.timeago.justNow.txt() else I18nKey.timeago.rightNow.txt()
    else if seconds < 60 then
      plural("second", seconds)
    else if minutes < 60 then
      plural("minute", minutes)
    else if hours < 24 then
      plural("hour", hours)
    else if days < 7 then
      plural("day", days)
    else if weeks < 4 then
      plural("week", weeks)
    else if months < 12 then
      plural("month", months)
    else
      plural("year", years)

  def daysFromNow(date: LocalDate)(using Translate): String =
    val today = nowInstant.date
    if date == today then I18nKey.site.today.txt()
    else if date == today.minusDays(1) then I18nKey.site.yesterday.txt()
    else momentFromNowServerText(date.atStartOfDay.instant)

  def timeRemaining(instant: Instant)(using Translate): Tag =
    // Use nbMinutesRemaining or nbHoursRemaining depending on the time left
    val nowSeconds = System.currentTimeMillis() / 1000
    val secondsRemaining = (instant.toMillis / 1000 - nowSeconds).toInt.atLeast(0)
    val minutes = secondsRemaining / 60
    val hours = minutes / 60

    val text =
      if minutes < 60 then I18nKey.timeago.nbMinutesRemaining.pluralSameTxt(minutes)
      else I18nKey.timeago.nbHoursRemaining.pluralSameTxt(hours)

    timeTag(cls := "timeago remaining", datetimeAttr := isoDateTime(instant))(text)

