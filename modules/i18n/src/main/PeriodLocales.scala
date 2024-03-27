package lila.i18n

import play.api.i18n.Lang
import java.time.Duration
import lila.hub.i18n.I18nKey

object PeriodLocales:

  def showDuration(duration: Duration)(using Lang): String =
    List(
      (I18nKey.nbDays, true, duration.toDays),
      (I18nKey.nbHours, true, duration.toHours      % 24),
      (I18nKey.nbMinutes, false, duration.toMinutes % 60)
    )
      .dropWhile { (_, dropZero, nb) => dropZero && nb == 0 }
      .map { (key, _, nb) => key.pluralSameTxt(nb) }
      .mkString(" ")
