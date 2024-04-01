package lila.opening

export lila.Core.{ *, given }

private val logger = lila.log("opening")

type PopularityHistoryAbsolute = List[Long]
type PopularityHistoryPercent  = List[Float]
