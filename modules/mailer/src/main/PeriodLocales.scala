package lila.mailer

import play.api.i18n.Lang
import java.time.Duration
import lila.core.i18n.{ Translate, I18nKey }

object PeriodLocales:

  def showDuration(duration: Duration)(using Translate): String =
    List(
      (I18nKey.site.nbDays, true, duration.toDays),
      (I18nKey.site.nbHours, true, duration.toHours      % 24),
      (I18nKey.site.nbMinutes, false, duration.toMinutes % 60)
    )
      .dropWhile { (_, dropZero, nb) => dropZero && nb == 0 }
      .map { (key, _, nb) => key.pluralSameTxt(nb) }
      .mkString(" ")
