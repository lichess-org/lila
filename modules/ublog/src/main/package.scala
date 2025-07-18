package lila.ublog

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val logger = lila.log("ublog")

val monthOfFirstPost = java.time.YearMonth.of(2021, 9)
