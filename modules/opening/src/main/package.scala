package lila.opening

export lila.Lila.{ *, given }

private val logger = lila.log("opening")

type PopularityHistoryAbsolute = List[Long]
type PopularityHistoryPercent  = List[Float]
