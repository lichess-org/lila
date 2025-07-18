package lila.ublog

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val logger = lila.log("ublog")

val monthOfFirstPost = java.time.YearMonth.of(2021, 9)

import lila.core.ublog.{ Quality, QualityFilter }
extension (qf: QualityFilter)
  def quality         = if qf == QualityFilter.all then Quality.weak else Quality.good
  def offTopicQuality = if qf == QualityFilter.all then Quality.spam else Quality.weak
