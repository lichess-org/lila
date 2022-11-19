package lila.tutor

import lila.insight.ClockPercent

export lila.Lila.{ *, given }

private val logger = lila.log("tutor")

private given Ordering[ClockPercent] = Ordering.by[ClockPercent, Double](_.value)
