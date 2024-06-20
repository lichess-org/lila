package lila.forumSearch

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val logger = lila.log("forumSearch")

val index = lila.search.Index.Forum
