package lila.i18n

import java.util.Locale
import org.joda.time.format.{ PeriodFormat, PeriodFormatter }
import org.joda.time.{ DurationFieldType, PeriodType, Period as JodaPeriod }
import play.api.i18n.Lang
import java.time.{ Duration, Period }

// TODO replace with threeten-extra?
// https://www.threeten.org/threeten-extra/apidocs/org.threeten.extra/org/threeten/extra/AmountFormats.html
object PeriodLocales:

  private val periodType = PeriodType forFields Array(
    DurationFieldType.days,
    DurationFieldType.hours,
    DurationFieldType.minutes
  )

  private def periodFormatter(using lang: Lang): PeriodFormatter =
    PeriodFormat wordBased lang.locale

  def showPeriod(period: Period)(using Lang): String =
    periodFormatter print JodaPeriod(period).normalizedStandard(periodType)

  def showDuration(duration: Duration)(using Lang): String =
    periodFormatter print JodaPeriod(duration.toMillis).normalizedStandard(periodType)
