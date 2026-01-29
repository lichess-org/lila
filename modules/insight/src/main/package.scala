package lila.insight

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val logger = lila.log("insight")

val maxGames = Max(10_000)
val minDate = instantOf(2023, 1, 1, 0, 0)
