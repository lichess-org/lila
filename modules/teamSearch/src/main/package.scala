package lila.teamSearch

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val logger = lila.log("teamSearch")

val index = lila.search.spec.Index.Team
