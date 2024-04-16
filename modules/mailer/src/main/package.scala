package lila.mailer

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val logger = lila.log("mailer")

import lila.core.i18n.{ Translate, I18nKey }

val translateDuration: lila.core.i18n.TranslateDuration = duration =>
  List(
    (I18nKey.site.nbDays, true, duration.toDays),
    (I18nKey.site.nbHours, true, duration.toHours      % 24),
    (I18nKey.site.nbMinutes, false, duration.toMinutes % 60)
  )
    .dropWhile { (_, dropZero, nb) => dropZero && nb == 0 }
    .map { (key, _, nb) => key.pluralSameTxt(nb) }
    .mkString(" ")
