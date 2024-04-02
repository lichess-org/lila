package lila.challenge

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private def inTwoWeeks = nowInstant.plusWeeks(2)

val logger = lila.log("challenge")
