package lila.studySearch

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val logger = lila.log("studySearch")

private val index = lila.search.Index.Study
