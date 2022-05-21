package lila.i18n

import java.util.Locale
import org.joda.time.format.{ PeriodFormat, PeriodFormatter }
import org.joda.time.{ DurationFieldType, Period, PeriodType }
import play.api.i18n.Lang
import scala.collection.mutable

object PeriodLocales {

  private val periodFormatters = mutable.AnyRefMap.empty[String, PeriodFormatter]

  private val periodType = PeriodType forFields Array(
    DurationFieldType.days,
    DurationFieldType.hours,
    DurationFieldType.minutes
  )

  private def periodFormatter(implicit lang: Lang): PeriodFormatter =
    periodFormatters.getOrElseUpdate(
      lang.code, {
        Locale setDefault Locale.ENGLISH
        PeriodFormat wordBased lang.toLocale
      }.assertNotNull
    )

  def showPeriod(period: Period)(implicit lang: Lang): String =
    periodFormatter print period.normalizedStandard(periodType)
}
