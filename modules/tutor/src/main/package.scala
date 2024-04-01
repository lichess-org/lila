package lila.tutor

import lila.insight.ClockPercent
import lila.user.User

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val logger = lila.log("tutor")

private given Ordering[lila.analyse.AccuracyPercent] = doubleOrdering
private given Ordering[ClockPercent]                 = doubleOrdering
private given Ordering[IntRating]                    = intOrdering
private given Ordering[GoodPercent]                  = doubleOrdering

private given Conversion[User.WithPerfs, User] = _.user

private def roundToInt(d: Double) = Math.round(d).toInt
