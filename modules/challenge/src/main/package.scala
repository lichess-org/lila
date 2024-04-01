package lila.challenge

export lila.Core.{ *, given }

private def inTwoWeeks = nowInstant.plusWeeks(2)

val logger = lila.log("challenge")
