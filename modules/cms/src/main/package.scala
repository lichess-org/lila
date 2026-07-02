package lila.cms

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

val markdownOptions = lila.memo.MarkdownOptions(
  autoLink = true,
  list = true,
  table = true,
  header = true,
  strikeThrough = true,
  blockQuote = true,
  code = true,
  timestamp = false,
  maxPgns = lila.memo.Max(50),
  toastUi = true
)
