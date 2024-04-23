package lila.ui

import lila.core.i18n.I18nKey
import lila.core.perf.{ KeyedPerf, UserPerfs }

export lila.core.lilaism.Lilaism.{ *, given }

trait RatingApi:
  val toNameKey: PerfKey => I18nKey
  val toIcon: PerfKey => Icon
  val bestRated: UserPerfs => Option[KeyedPerf]
