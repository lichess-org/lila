package lila

import lila.insight.ClockPercent

package object tutor extends lila.PackageObject:

  private val logger = lila.log("tutor")

  private given clockPercentOrdering: Ordering[ClockPercent] =
    Ordering.by[ClockPercent, Double](_.value)
