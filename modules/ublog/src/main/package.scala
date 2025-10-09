package lila.ublog

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val logger = lila.log("ublog")

val monthOfFirstPost = java.time.YearMonth.of(2021, 9)

val markdownOptions = lila.memo.MarkdownOptions.all.copy(
  maxPgns = lila.memo.Max(50),
  toastUi = true,
  imageDesignWidth = 800.some
)
