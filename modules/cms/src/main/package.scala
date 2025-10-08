package lila.cms

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val logger = lila.log("cms")

val markdownOptions = lila.memo.MarkdownOptions.all.copy(maxPgns = lila.memo.Max(50), imageUpload = true)
