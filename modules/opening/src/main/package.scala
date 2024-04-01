package lila.opening

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val logger = lila.log("opening")

type PopularityHistoryAbsolute = List[Long]
type PopularityHistoryPercent  = List[Float]
