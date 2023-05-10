package lila.i18n

import play.api.i18n.Lang
import java.time.Duration

object PeriodLocales:

  def showDuration(duration: Duration)(using Lang): String =
    List(
      (I18nKeys.nbDays, true, duration.toDays),
      (I18nKeys.nbHours, true, duration.toHours      % 24),
      (I18nKeys.nbMinutes, false, duration.toMinutes % 60)
    )
      .dropWhile { case (_, dropZero, nb) => dropZero && nb == 0 }
      .map { case (key, _, nb) => key.pluralSameTxt(nb) }
      .mkString(" ")
